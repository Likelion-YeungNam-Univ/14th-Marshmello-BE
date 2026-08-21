package Marshmello.MarshmelloWas.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;

class SwaggerDefaultPolicyTest {

    private static final String API_DOCS_ENABLED = "springdoc.api-docs.enabled";
    private static final String SWAGGER_UI_ENABLED = "springdoc.swagger-ui.enabled";

    @Test
    void baseRuntimeDisablesBothSwaggerSurfacesByDefault() throws IOException {
        PropertySourcesPropertyResolver resolver = resolver(
                loadProperties("src/main/resources/application.properties"),
                Map.of());

        assertThat(resolver.getRequiredProperty(API_DOCS_ENABLED, Boolean.class)).isFalse();
        assertThat(resolver.getRequiredProperty(SWAGGER_UI_ENABLED, Boolean.class)).isFalse();
    }

    @Test
    void explicitEnvironmentOverrideEnablesBothSwaggerSurfaces() throws IOException {
        PropertySourcesPropertyResolver resolver = resolver(
                loadProperties("src/main/resources/application.properties"),
                Map.of("SWAGGER_ENABLED", "true"));

        assertThat(resolver.getRequiredProperty(API_DOCS_ENABLED, Boolean.class)).isTrue();
        assertThat(resolver.getRequiredProperty(SWAGGER_UI_ENABLED, Boolean.class)).isTrue();
    }

    @Test
    void localProfileExplicitlyKeepsSwaggerEnabledByDefault() throws IOException {
        Properties local = loadProperties("src/main/resources/application-local.properties");

        assertThat(local)
                .containsEntry(API_DOCS_ENABLED, "${SWAGGER_ENABLED:true}")
                .containsEntry(SWAGGER_UI_ENABLED, "${SWAGGER_ENABLED:true}");
    }

    private static Properties loadProperties(String path) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(Path.of(path))) {
            properties.load(input);
        }
        return properties;
    }

    private static PropertySourcesPropertyResolver resolver(
            Properties application,
            Map<String, Object> environment
    ) {
        MutablePropertySources propertySources = new MutablePropertySources();
        propertySources.addFirst(new MapPropertySource("fixture-environment", environment));
        propertySources.addLast(new PropertiesPropertySource("application", application));
        return new PropertySourcesPropertyResolver(propertySources);
    }
}
