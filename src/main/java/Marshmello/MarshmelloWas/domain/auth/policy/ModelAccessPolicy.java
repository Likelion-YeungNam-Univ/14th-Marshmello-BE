package Marshmello.MarshmelloWas.domain.auth.policy;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import Marshmello.MarshmelloWas.domain.auth.model.AuthenticatedPrincipal;
import Marshmello.MarshmelloWas.domain.auth.model.ModelAuthorizationRules;

public final class ModelAccessPolicy {

    private final boolean requireVerifiedEmail;
    private final Set<String> allowedEmails;
    private final Set<String> allowedDomains;
    private final Set<String> allowedSubjects;

    public ModelAccessPolicy(ModelAuthorizationRules rules) {
        requireVerifiedEmail = rules.requireVerifiedEmail();
        allowedEmails = normalize(rules.allowedEmails());
        allowedDomains = normalize(rules.allowedEmailDomains());
        allowedSubjects = rules.allowedSubjects().stream()
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isAllowed(AuthenticatedPrincipal principal) {
        if (principal.subject() != null && allowedSubjects.contains(principal.subject())) {
            return true;
        }

        String email = normalize(principal.email());
        if (requireVerifiedEmail && email != null && !principal.emailVerified()) {
            return false;
        }
        if (email != null && allowedEmails.contains(email)) {
            return true;
        }

        String domain = emailDomain(email);
        return domain != null && allowedDomains.contains(domain);
    }

    private static Set<String> normalize(Iterable<String> values) {
        HashSet<String> normalized = new HashSet<>();
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
