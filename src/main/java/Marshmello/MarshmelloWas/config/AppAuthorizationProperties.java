package Marshmello.MarshmelloWas.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.authorization")
public record AppAuthorizationProperties(
        List<String> allowedEmails,
        List<String> allowedEmailDomains,
        List<String> allowedSubjects,
        boolean requireVerifiedEmail
) {
    public AppAuthorizationProperties {
        allowedEmails = allowedEmails == null ? List.of() : List.copyOf(allowedEmails);
        allowedEmailDomains = allowedEmailDomains == null ? List.of() : List.copyOf(allowedEmailDomains);
        allowedSubjects = allowedSubjects == null ? List.of() : List.copyOf(allowedSubjects);
    }
}
