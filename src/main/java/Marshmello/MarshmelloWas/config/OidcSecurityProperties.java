package Marshmello.MarshmelloWas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.oidc")
public record OidcSecurityProperties(
        boolean googleOfflineAccess
) {
}
