package Marshmello.MarshmelloWas.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import Marshmello.MarshmelloWas.domain.auth.entity.SocialAccountId;
import Marshmello.MarshmelloWas.domain.auth.repository.SocialAccountRepository;
import Marshmello.MarshmelloWas.domain.user.entity.User;
import Marshmello.MarshmelloWas.domain.user.repository.UserRepository;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class OidcUserProvisioningServiceTest {

    @Autowired
    private OidcUserProvisioningService provisioningService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Test
    void createsPendingUserAndSocialAccountOnFirstLogin() {
        long userId = provisioningService.provision("oidc", "new-subject");

        User user = userRepository.findById(userId).orElseThrow();
        assertThat(user.getNickname()).isEqualTo("마시멜로");
        assertThat(user.getExpectedDeliveryDate()).isNull();
        assertThat(user.isProfileCompleted()).isFalse();
        assertThat(socialAccountRepository.findById(
                new SocialAccountId("oidc", "new-subject")))
                .get()
                .extracting(account -> account.getUserId())
                .isEqualTo(userId);
    }

    @Test
    void reusesExistingUserOnRepeatedLogin() {
        long firstUserId = provisioningService.provision("oidc", "same-subject");
        long userCount = userRepository.count();

        long secondUserId = provisioningService.provision("oidc", "same-subject");

        assertThat(secondUserId).isEqualTo(firstUserId);
        assertThat(userRepository.count()).isEqualTo(userCount);
    }

    @Test
    void keepsSameSubjectSeparateBetweenProviders() {
        long firstUserId = provisioningService.provision("first", "shared-subject");
        long secondUserId = provisioningService.provision("second", "shared-subject");

        assertThat(secondUserId).isNotEqualTo(firstUserId);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void createsOnlyOneUserForConcurrentFirstLogin() throws Exception {
        SocialAccountId accountId = new SocialAccountId("oidc", "concurrent-subject");
        long userCountBefore = userRepository.count();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<Long> first = executor.submit(() -> {
                ready.countDown();
                start.await();
                return provisioningService.provision("oidc", "concurrent-subject");
            });
            Future<Long> second = executor.submit(() -> {
                ready.countDown();
                start.await();
                return provisioningService.provision("oidc", "concurrent-subject");
            });

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            long firstUserId = first.get(10, TimeUnit.SECONDS);
            long secondUserId = second.get(10, TimeUnit.SECONDS);

            assertThat(secondUserId).isEqualTo(firstUserId);
            assertThat(userRepository.count()).isEqualTo(userCountBefore + 1);
        } finally {
            executor.shutdownNow();
            socialAccountRepository.findById(accountId).ifPresent(account -> {
                socialAccountRepository.delete(account);
                userRepository.deleteById(account.getUserId());
            });
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rollsBackUserWhenSocialAccountCannotBeStored() {
        long userCountBefore = userRepository.count();
        String oversizedProvider = "p".repeat(51);

        assertThatThrownBy(() ->
                provisioningService.provision(oversizedProvider, "provider-user-1"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(userRepository.count()).isEqualTo(userCountBefore);
    }
}
