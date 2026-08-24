package Marshmello.MarshmelloWas.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class RequiredEnvironmentConfigurationTest {

    @Test
    void requiresSecurityValuesButKeepsDatabaseDefaults() throws IOException {
        Properties application = loadProperties("src/main/resources/application.properties");

        assertThat(application)
                .containsEntry(
                        "spring.datasource.url",
                        "${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/marshmello}")
                .containsEntry("spring.datasource.username", "${SPRING_DATASOURCE_USERNAME:marshmello}")
                .containsEntry("spring.datasource.password", "${SPRING_DATASOURCE_PASSWORD:}")
                .containsEntry("spring.security.oauth2.client.registration.oidc.client-id", "${OIDC_CLIENT_ID}")
                .containsEntry("spring.security.oauth2.client.registration.oidc.client-secret", "${OIDC_CLIENT_SECRET}")
                .containsEntry("spring.security.oauth2.client.provider.oidc.issuer-uri", "${OIDC_ISSUER_URI}")
                .containsEntry("app.cors.allowed-origins", "${APP_CORS_ALLOWED_ORIGINS}")
                .containsEntry("app.login-success-url", "${APP_LOGIN_SUCCESS_URL}");
    }

    @Test
    void testRuntimeSuppliesRequiredApplicationSecurityValues() throws IOException {
        Properties testApplication = loadProperties("src/test/resources/application.properties");

        assertThat(testApplication)
                .containsEntry("app.cors.allowed-origins", "http://localhost:5173")
                .containsEntry("app.login-success-url", "http://localhost:5173");
    }

    private static Properties loadProperties(String path) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(Path.of(path))) {
            properties.load(input);
        }
        return properties;
    }
}
