package Marshmello.MarshmelloWas.domain.auth.adapter;

import Marshmello.MarshmelloWas.domain.auth.entity.SocialAccount;
import Marshmello.MarshmelloWas.domain.auth.entity.SocialAccountId;
import Marshmello.MarshmelloWas.domain.auth.port.CurrentUserIdProvider;
import Marshmello.MarshmelloWas.domain.auth.repository.SocialAccountRepository;
import Marshmello.MarshmelloWas.global.exception.ApiException;
import Marshmello.MarshmelloWas.global.exception.AuthenticationRequiredException;
import Marshmello.MarshmelloWas.global.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class OidcCurrentUserIdProvider implements CurrentUserIdProvider {

    private final SocialAccountRepository socialAccountRepository;

    public OidcCurrentUserIdProvider(SocialAccountRepository socialAccountRepository) {
        this.socialAccountRepository = socialAccountRepository;
    }

    @Override
    public long requireCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof OAuth2AuthenticationToken oauth2Authentication)
                || !authentication.isAuthenticated()) {
            throw new AuthenticationRequiredException();
        }

        SocialAccountId accountId = new SocialAccountId(
                oauth2Authentication.getAuthorizedClientRegistrationId(),
                oauth2Authentication.getName());

        return socialAccountRepository.findById(accountId)
                .map(SocialAccount::getUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }
}
