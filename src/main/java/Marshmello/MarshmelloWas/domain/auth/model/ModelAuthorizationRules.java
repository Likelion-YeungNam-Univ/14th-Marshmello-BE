package Marshmello.MarshmelloWas.domain.auth.model;

import java.util.List;

public record ModelAuthorizationRules(
        List<String> allowedEmails,
        List<String> allowedEmailDomains,
        List<String> allowedSubjects,
        boolean requireVerifiedEmail
) {
    public ModelAuthorizationRules {
        allowedEmails = List.copyOf(allowedEmails);
        allowedEmailDomains = List.copyOf(allowedEmailDomains);
        allowedSubjects = List.copyOf(allowedSubjects);
    }
}
