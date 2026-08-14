package Marshmello.MarshmelloWas.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "springdoc.api-docs.enabled=true",
                "springdoc.swagger-ui.enabled=true",
                "springdoc.paths-to-match=/api/**"
        })
@Import(OpenApiAutoDiscoveryHttpQaTest.SwaggerProbeTestConfiguration.class)
class OpenApiAutoDiscoveryHttpQaTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void discoversOnlyApiMappingsAndHidesInjectedCsrfTokenFromLiveDocument() throws Exception {
        HttpResponse<String> response = get("/v3/api-docs");
        JsonNode document = objectMapper.readTree(response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertThat(document.path("info").path("title").asText()).isEqualTo("Marshmello WAS API");
        assertThat(document.path("info").path("version").asText()).isEqualTo("v1");
        assertThat(document.path("paths").has("/api/swagger-probe")).isTrue();
        assertThat(document.path("paths").has("/internal/swagger-probe")).isFalse();
        assertThat(document.path("paths").has("/api/csrf")).isTrue();
        assertThat(document.path("paths").path("/api/csrf").path("get").path("parameters").size())
                .isZero();
        System.out.println("OPENAPI_HTTP_SUMMARY status=200 json=true title=Marshmello WAS API "
                + "version=v1 apiProbe=true internalProbe=false csrfParameterCount=0");
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SwaggerProbeTestConfiguration {

        @Bean
        @Order(0)
        SecurityFilterChain swaggerTestSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                    .securityMatcher("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                    .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                    .csrf(AbstractHttpConfigurer::disable);
            return http.build();
        }

        @Bean
        SwaggerApiProbeController swaggerApiProbeController() {
            return new SwaggerApiProbeController();
        }

        @Bean
        SwaggerInternalProbeController swaggerInternalProbeController() {
            return new SwaggerInternalProbeController();
        }
    }

    @RestController
    static class SwaggerApiProbeController {

        @GetMapping("/api/swagger-probe")
        void apiProbe() {
        }
    }

    @RestController
    static class SwaggerInternalProbeController {

        @GetMapping("/internal/swagger-probe")
        void internalProbe() {
        }
    }
}
