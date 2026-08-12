package Marshmello.MarshmelloWas.auth;

import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import Marshmello.MarshmelloWas.config.AppAuthorizationProperties;

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

    private final AppAuthorizationProperties properties;
    private final Set<String> allowedEmails;
    private final Set<String> allowedDomains;
    private final Set<String> allowedSubjects;

    public ModelGateAuthorizationManager(AppAuthorizationProperties properties) {
        this.properties = properties;
        this.allowedEmails = normalize(properties.allowedEmails());
        this.allowedDomains = normalize(properties.allowedEmailDomains());
        this.allowedSubjects = properties.allowedSubjects().stream()
            .filter(value -> value != null && !value.isBlank())
            .collect(Collectors.toUnmodifiableSet());
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

        // A stable OIDC subject can be authorized with the minimal `openid` scope.
        // Email verification is only relevant when authorization depends on email.
        String subject = user.getSubject();
        if (subject != null && allowedSubjects.contains(subject)) {
            return new AuthorizationDecision(true);
        }

        String email = normalize(user.getEmail());
        if (properties.requireVerifiedEmail()
                && email != null
                && !Boolean.TRUE.equals(user.getEmailVerified())) {
            return new AuthorizationDecision(false);
        }
        if (email != null && allowedEmails.contains(email)) {
            return new AuthorizationDecision(true);
        }

        String domain = emailDomain(email);
        if (domain != null && allowedDomains.contains(domain)) {
            return new AuthorizationDecision(true);
        }

        // Fail closed when no rule matches (including an empty allow-list).
        return new AuthorizationDecision(false);
    }

    private static Set<String> normalize(Iterable<String> values) {
        java.util.HashSet<String> normalized = new java.util.HashSet<>();
        for (String value : values) {
            String item = normalize(value);
            if (item != null) {
                normalized.add(item);
            }
        }
        return Set.copyOf(normalized);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String emailDomain(String email) {
        if (email == null) {
            return null;
        }
        int at = email.lastIndexOf('@');
        if (at <= 0 || at == email.length() - 1) {
            return null;
        }
        return email.substring(at + 1);
    }
}
