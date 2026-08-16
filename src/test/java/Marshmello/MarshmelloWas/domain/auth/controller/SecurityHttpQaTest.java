package Marshmello.MarshmelloWas.domain.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.login-success-url=http://localhost:5173",
                "spring.security.oauth2.client.provider.test-provider.user-info-uri="
        })
@Import(SecurityHttpQaTest.OAuthCallbackTestConfiguration.class)
class SecurityHttpQaTest {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .cookieHandler(new CookieManager())
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
    void allowsConfiguredOriginPreflightForProtectedApiBeforeAuthentication() throws Exception {
        HttpResponse<String> response = options("/api/me", "http://localhost:5173");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .hasValue("http://localhost:5173");
        assertThat(response.headers().firstValue(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS))
                .hasValue("true");
    }

    @Test
    void exposesCsrfTokenToConfiguredOrigin() throws Exception {
        HttpResponse<String> response = get("/api/csrf", "http://localhost:5173");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .hasValue("http://localhost:5173");
        assertThat(response.headers().firstValue(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS))
                .hasValue("true");
    }

    @Test
    void rejectsUnauthenticatedProtectedApiWithCorsHeadersAndWithoutRedirect() throws Exception {
        HttpResponse<String> response = get("/api/me", "http://localhost:5173");

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .hasValue("http://localhost:5173");
        assertThat(response.headers().firstValue(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS))
                .hasValue("true");
        assertThat(response.headers().firstValue(HttpHeaders.LOCATION)).isEmpty();
    }

    @Test
    void rejectsAttackerOriginPreflightForProtectedApi() throws Exception {
        HttpResponse<String> response = options("/api/me", "https://attacker.example.test");

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.headers().firstValue(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEmpty();
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

    @Test
    void redirectsOauthLoginSuccessToConfiguredUrl() throws Exception {
        HttpResponse<String> authorization = get("/oauth2/authorization/oidc");
        String state = URI.create(authorization.headers().firstValue(HttpHeaders.LOCATION).orElseThrow())
                .getRawQuery()
                .replaceFirst(".*(?:^|&)state=([^&]+).*", "$1");
        HttpResponse<String> callback = get("/login/oauth2/code/oidc?code=test-code&state=" + state);

        assertThat(authorization.statusCode()).isEqualTo(302);
        assertThat(callback.statusCode()).isEqualTo(302);
        assertThat(callback.headers().firstValue(HttpHeaders.LOCATION))
                .hasValue("http://localhost:5173");
    }

    private HttpResponse<String> get(String path) throws Exception {
        return get(path, null);
    }

    private HttpResponse<String> get(String path, String origin) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .GET();
        if (origin != null) {
            request.header(HttpHeaders.ORIGIN, origin);
        }
        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> options(String path, String origin) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .header(HttpHeaders.ORIGIN, origin)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name())
                .method(HttpMethod.OPTIONS.name(), HttpRequest.BodyPublishers.noBody())
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class OAuthCallbackTestConfiguration {

        @Bean
        OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
            return request -> {
                var authorizationRequest = request.getAuthorizationExchange().getAuthorizationRequest();
                String nonce = authorizationRequest
                        .getAdditionalParameters()
                        .get("nonce")
                        .toString();
                String codeVerifier = authorizationRequest.getAttribute(PkceParameterNames.CODE_VERIFIER);
                assertThat(codeVerifier).isNotBlank();
                return OAuth2AccessTokenResponse.withToken("test-token")
                        .tokenType(OAuth2AccessToken.TokenType.BEARER)
                        .expiresIn(300)
                        .additionalParameters(Map.of("id_token", "test-id-token:" + nonce))
                        .build();
            };
        }

        @Bean
        JwtDecoderFactory<ClientRegistration> jwtDecoderFactory() {
            return registration -> token -> {
                Instant issuedAt = Instant.now();
                return new Jwt(
                        token,
                        issuedAt,
                        issuedAt.plusSeconds(300),
                        Map.of("alg", "none"),
                        Map.of(
                                "sub", "subject",
                                "aud", List.of(registration.getClientId()),
                                "nonce", token.substring("test-id-token:".length())));
            };
        }
    }
}
