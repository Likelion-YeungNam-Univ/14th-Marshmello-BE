package Marshmello.MarshmelloWas.infrastructure.auth.security;

import java.util.function.Supplier;

import Marshmello.MarshmelloWas.domain.auth.model.AuthenticatedPrincipal;
import Marshmello.MarshmelloWas.domain.auth.model.ModelAuthorizationRules;
import Marshmello.MarshmelloWas.domain.auth.policy.ModelAccessPolicy;
import Marshmello.MarshmelloWas.infrastructure.auth.config.AppAuthorizationProperties;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

@Component
public final class ModelGateAuthorizationManager
        implements AuthorizationManager<RequestAuthorizationContext> {

    private final ModelAccessPolicy modelAccessPolicy;

    public ModelGateAuthorizationManager(AppAuthorizationProperties properties) {
        modelAccessPolicy = new ModelAccessPolicy(new ModelAuthorizationRules(
                properties.allowedEmails(),
                properties.allowedEmailDomains(),
                properties.allowedSubjects(),
                properties.requireVerifiedEmail()));
    }

    @Override
    public AuthorizationResult authorize(
            Supplier<? extends Authentication> authenticationSupplier,
            RequestAuthorizationContext context
    ) {
        Authentication authentication = authenticationSupplier.get();
        if (authentication == null || !authentication.isAuthenticated()) {
            return new AuthorizationDecision(false);
        }
        if (!(authentication.getPrincipal() instanceof OidcUser user)) {
            return new AuthorizationDecision(false);
        }

        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(
                user.getSubject(),
                user.getEmail(),
                Boolean.TRUE.equals(user.getEmailVerified()));
        return new AuthorizationDecision(modelAccessPolicy.isAllowed(principal));
    }
}
