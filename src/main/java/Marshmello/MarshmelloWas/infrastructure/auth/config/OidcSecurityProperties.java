package Marshmello.MarshmelloWas.infrastructure.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.oidc")
public record OidcSecurityProperties(
        boolean googleOfflineAccess
) {
}
