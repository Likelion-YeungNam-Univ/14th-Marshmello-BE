package Marshmello.MarshmelloWas.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

class CorsAllowedOriginsTest {

    @Test
    void normalizesConfiguredOriginsOnceIntoAnImmutableOrderedList() {
        CorsAllowedOrigins origins = new CorsAllowedOrigins(
                " HTTPS://APP.example.test, ,https://admin.example.test,HTTPS://APP.example.test ");

        assertThat(origins.values())
                .containsExactly("HTTPS://APP.example.test", "https://admin.example.test");
        assertThat(origins.match("https://app.EXAMPLE.test/login"))
                .contains("https://app.example.test");
        assertThat(origins.match("https://attacker.example.test")).isEmpty();
        assertThatThrownBy(() -> origins.values().add("https://other.example.test"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void springConstructionRequiresTheConfiguredOriginsProperty() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.addBeanFactoryPostProcessor(new PropertySourcesPlaceholderConfigurer());
            context.register(CorsAllowedOrigins.class);

            assertThatThrownBy(context::refresh)
                    .hasRootCauseInstanceOf(IllegalArgumentException.class)
                    .hasStackTraceContaining("app.cors.allowed-origins");
        }
    }
}
