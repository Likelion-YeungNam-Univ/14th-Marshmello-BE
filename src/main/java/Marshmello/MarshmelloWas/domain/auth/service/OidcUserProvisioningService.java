package Marshmello.MarshmelloWas.domain.auth.service;

import Marshmello.MarshmelloWas.domain.auth.entity.SocialAccount;
import Marshmello.MarshmelloWas.domain.auth.entity.SocialAccountId;
import Marshmello.MarshmelloWas.domain.auth.repository.SocialAccountRepository;
import Marshmello.MarshmelloWas.domain.user.entity.User;
import Marshmello.MarshmelloWas.domain.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OidcUserProvisioningService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final TransactionTemplate transactionTemplate;

    public OidcUserProvisioningService(
            UserRepository userRepository,
            SocialAccountRepository socialAccountRepository,
            PlatformTransactionManager transactionManager) {
        this.userRepository = userRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public long provision(String provider, String providerUserId) {
        SocialAccountId accountId = new SocialAccountId(provider, providerUserId);

        return socialAccountRepository.findById(accountId)
                .map(SocialAccount::getUserId)
                .orElseGet(() -> createAccount(accountId));
    }

    private long createAccount(SocialAccountId accountId) {
        try {
            Long userId = transactionTemplate.execute(status ->
                    socialAccountRepository.findById(accountId)
                            .map(SocialAccount::getUserId)
                            .orElseGet(() -> createUserAndAccount(accountId)));

            if (userId == null) {
                throw new IllegalStateException("OIDC 사용자 생성 트랜잭션 결과가 없습니다.");
            }
            return userId;
        } catch (DataIntegrityViolationException exception) {
            return socialAccountRepository.findById(accountId)
                    .map(SocialAccount::getUserId)
                    .orElseThrow(() -> exception);
        }
    }

    private Long createUserAndAccount(SocialAccountId accountId) {
        User user = userRepository.save(User.createPendingProfile());
        socialAccountRepository.saveAndFlush(new SocialAccount(accountId, user.getUserId()));
        return user.getUserId();
    }
}
