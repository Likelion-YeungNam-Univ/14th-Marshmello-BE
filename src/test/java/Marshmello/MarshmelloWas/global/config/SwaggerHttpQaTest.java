package Marshmello.MarshmelloWas.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import Marshmello.MarshmelloWas.MarshmelloWasApplication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.http.MediaType;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "springdoc.api-docs.enabled=true",
                "springdoc.swagger-ui.enabled=true",
                "springdoc.paths-to-match=/api/**"
        })
@Import(OpenApiAutoDiscoveryHttpQaTest.SwaggerProbeTestConfiguration.class)
class SwaggerHttpQaTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Test
    void returnsSwaggerUiHtmlThroughTheIsolatedTestSecurityChain() throws Exception {
        HttpResponse<String> response = get("/swagger-ui/index.html");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .startsWith(MediaType.TEXT_HTML_VALUE);
        System.out.println("SWAGGER_UI_HTTP_SUMMARY status=200 html=true");
    }

    @Test
    void rejectsMalformedSwaggerEnabledValueDuringIsolatedApplicationStartup() {
        assertThatThrownBy(() -> {
            try (ConfigurableApplicationContext ignored = new SpringApplicationBuilder(
                    MarshmelloWasApplication.class)
                    .web(WebApplicationType.SERVLET)
                    .properties(
                            "server.port=0",
                            "SWAGGER_ENABLED=not-a-boolean",
                            "springdoc.api-docs.enabled=${SWAGGER_ENABLED:false}",
                            "springdoc.swagger-ui.enabled=${SWAGGER_ENABLED:false}",
                            "springdoc.paths-to-match=/api/**")
                    .run()) {
            }
        }).hasRootCauseInstanceOf(SpelEvaluationException.class);
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
