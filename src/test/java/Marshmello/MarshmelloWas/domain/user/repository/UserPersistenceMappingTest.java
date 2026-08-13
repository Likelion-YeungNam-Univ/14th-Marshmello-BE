package Marshmello.MarshmelloWas.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import Marshmello.MarshmelloWas.domain.auth.entity.SocialAccount;
import Marshmello.MarshmelloWas.domain.auth.entity.SocialAccountId;
import Marshmello.MarshmelloWas.domain.auth.repository.SocialAccountRepository;
import Marshmello.MarshmelloWas.domain.user.entity.User;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserPersistenceMappingTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Test
    void keepsAuthenticationIdentitySeparateFromUserProfile() {
        LocalDate expectedDeliveryDate = LocalDate.of(2027, 1, 15);
        User user = userRepository.save(new User("marshmello", expectedDeliveryDate));
        SocialAccountId accountId = new SocialAccountId("oidc", "provider-user-1");
        socialAccountRepository.save(new SocialAccount(accountId, user.getUserId()));

        SocialAccount socialAccount = socialAccountRepository.findById(accountId).orElseThrow();
        User savedUser = userRepository.findById(socialAccount.getUserId()).orElseThrow();

        assertThat(savedUser.getNickname()).isEqualTo("marshmello");
        assertThat(savedUser.getExpectedDeliveryDate()).isEqualTo(expectedDeliveryDate);
        assertThat(savedUser.isProfileCompleted()).isTrue();
        assertThat(socialAccount.getSocialAccountId()).isEqualTo(accountId);
        assertThat(socialAccountRepository.findByUserId(savedUser.getUserId()))
                .containsExactly(socialAccount);
    }
}
