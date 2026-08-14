package Marshmello.MarshmelloWas.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "springdoc.api-docs.enabled=true",
                "springdoc.swagger-ui.enabled=true",
                "springdoc.paths-to-match=/api/**"
        })
class SwaggerSecurityHttpQaTest {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void permitsOnlySwaggerSurfacesWithoutChangingTheExistingSecurityContract() throws Exception {
        HttpResponse<String> apiDocs = get("/v3/api-docs");
        HttpResponse<String> swaggerUi = get("/swagger-ui/index.html");
        HttpResponse<String> protectedApi = get("/api/me");
        HttpResponse<String> csrf = get("/api/csrf");
        HttpResponse<String> logoutWithoutCsrf = post("/logout");
        HttpResponse<String> swaggerLikeCss = get("/swagger-probe.css");

        assertThat(apiDocs.statusCode()).isEqualTo(200);
        assertThat(apiDocs.headers().firstValue("Content-Type").orElse(""))
                .startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertThat(objectMapper.readTree(apiDocs.body()).path("openapi").asText()).isNotBlank();
        assertThat(swaggerUi.statusCode()).isEqualTo(200);
        assertThat(swaggerUi.headers().firstValue("Content-Type").orElse(""))
                .startsWith(MediaType.TEXT_HTML_VALUE);
        assertThat(protectedApi.statusCode()).isEqualTo(401);
        assertThat(csrf.statusCode()).isEqualTo(200);
        assertThat(logoutWithoutCsrf.statusCode()).isEqualTo(403);
        assertThat(swaggerLikeCss.statusCode()).isEqualTo(401);
    }

    private HttpResponse<String> get(String path) throws Exception {
        return send(HttpRequest.newBuilder(uri(path)).GET().build());
    }

    private HttpResponse<String> post(String path) throws Exception {
        return send(HttpRequest.newBuilder(uri(path)).POST(HttpRequest.BodyPublishers.noBody()).build());
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
