package Marshmello.MarshmelloWas.infrastructure.auth.web;

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
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityHttpQaTest {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void exposesHealthWithoutAuthentication() throws Exception {
        HttpResponse<String> response = get("/actuator/health");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readTree(response.body()).path("status").asText()).isEqualTo("UP");
    }

    @Test
    void rejectsProtectedApiWithoutAuthentication() throws Exception {
        HttpResponse<String> response = get("/api/me");

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void exposesCsrfTokenWithoutAuthentication() throws Exception {
        HttpResponse<String> response = get("/api/csrf");

        JsonNode body = objectMapper.readTree(response.body());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(body.path("headerName").asText()).isEqualTo("X-CSRF-TOKEN");
        assertThat(body.path("parameterName").asText()).isEqualTo("_csrf");
        assertThat(body.path("token").asText()).isNotBlank();
    }

    @Test
    void startsOidcAuthorizationWithPkce() throws Exception {
        HttpResponse<String> response = get("/oauth2/authorization/oidc");

        URI location = URI.create(response.headers().firstValue("Location").orElseThrow());
        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(location.getHost()).isEqualTo("idp.example.test");
        assertThat(location.getRawQuery())
                .contains("state=", "nonce=", "code_challenge=", "code_challenge_method=S256");
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
