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
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
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
                "app.cors.allowed-origins= http://localhost:5173, , "
                        + "https://dev.dia8lj4ohc0fh.amplifyapp.com, http://localhost:5173 ",
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
        for (String origin : configuredOrigins()) {
            HttpResponse<String> response = options("/api/me", origin);

            assertThat(response.statusCode()).as(origin).isEqualTo(200);
            assertThat(response.headers().firstValue(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                    .as(origin)
                    .hasValue(origin);
            assertThat(response.headers().firstValue(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS))
                    .as(origin)
                    .hasValue("true");
        }
    }

    @Test
    void allowsTheSameConfiguredOriginPreflightForLogout() throws Exception {
        for (String origin : configuredOrigins()) {
            HttpResponse<String> response = options("/logout", origin, HttpMethod.POST);

            assertThat(response.statusCode()).as(origin).isEqualTo(200);
            assertThat(response.headers().firstValue(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                    .as(origin)
                    .hasValue(origin);
            assertThat(response.headers().firstValue(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS))
                    .as(origin)
                    .hasValue("true");
        }
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
    void rejectsUnlistedAttackerAndLookalikeOriginsForProtectedApi() throws Exception {
        for (String origin : rejectedOrigins()) {
            HttpResponse<String> response = options("/api/me", origin);

            assertThat(response.statusCode()).as(origin).isEqualTo(403);
            assertThat(response.headers().firstValue(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                    .as(origin)
                    .isEmpty();
        }
    }

    @Test
    void rejectsUnlistedAttackerAndLookalikeOriginsForLogout() throws Exception {
        for (String origin : rejectedOrigins()) {
            HttpResponse<String> response = options("/logout", origin, HttpMethod.POST);

            assertThat(response.statusCode()).as(origin).isEqualTo(403);
            assertThat(response.headers().firstValue(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                    .as(origin)
                    .isEmpty();
        }
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

    @Test
    void redirectsOauthLoginSuccessToTheFrontendThatStartedLogin() throws Exception {
        HttpClient amplifyClient = newHttpClient();
        HttpResponse<String> amplifyAuthorization = get(
                amplifyClient,
                "/oauth2/authorization/oidc",
                null,
                "https://dev.dia8lj4ohc0fh.amplifyapp.com/login");
        HttpResponse<String> amplifyCallback = get(
                amplifyClient,
                callbackPath(amplifyAuthorization),
                null,
                null);

        HttpClient localhostClient = newHttpClient();
        HttpResponse<String> localhostAuthorization = get(
                localhostClient,
                "/oauth2/authorization/oidc",
                null,
                "http://localhost:5173/login");
        HttpResponse<String> localhostCallback = get(
                localhostClient,
                callbackPath(localhostAuthorization),
                null,
                null);

        assertThat(amplifyCallback.statusCode()).isEqualTo(302);
        assertThat(amplifyCallback.headers().firstValue(HttpHeaders.LOCATION))
                .hasValue("https://dev.dia8lj4ohc0fh.amplifyapp.com");
        assertThat(localhostCallback.statusCode()).isEqualTo(302);
        assertThat(localhostCallback.headers().firstValue(HttpHeaders.LOCATION))
                .hasValue("http://localhost:5173");
    }

    @Test
    void ignoresUnconfiguredLoginOriginAndUsesConfiguredFallback() throws Exception {
        HttpClient attackerClient = newHttpClient();
        HttpResponse<String> authorization = get(
                attackerClient,
                "/oauth2/authorization/oidc",
                null,
                "https://attacker.example.test/login");
        HttpResponse<String> callback = get(
                attackerClient,
                callbackPath(authorization),
                null,
                null);

        assertThat(callback.statusCode()).isEqualTo(302);
        assertThat(callback.headers().firstValue(HttpHeaders.LOCATION))
                .hasValue("http://localhost:5173");
    }

    private String callbackPath(HttpResponse<String> authorization) {
        String state = URI.create(authorization.headers().firstValue(HttpHeaders.LOCATION).orElseThrow())
                .getRawQuery()
                .replaceFirst(".*(?:^|&)state=([^&]+).*", "$1");
        return "/login/oauth2/code/oidc?code=test-code&state=" + state;
    }

    private HttpResponse<String> get(String path) throws Exception {
        return get(path, null);
    }

    private HttpResponse<String> get(String path, String origin) throws Exception {
        return get(httpClient, path, origin, null);
    }

    private HttpResponse<String> get(
            HttpClient client,
            String path,
            String origin,
            String referer
    ) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .GET();
        if (origin != null) {
            request.header(HttpHeaders.ORIGIN, origin);
        }
        if (referer != null) {
            request.header(HttpHeaders.REFERER, referer);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpClient newHttpClient() {
        return HttpClient.newBuilder()
                .cookieHandler(new CookieManager())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    private static List<String> configuredOrigins() {
        return List.of(
                "http://localhost:5173",
                "https://dev.dia8lj4ohc0fh.amplifyapp.com");
    }

    private static List<String> rejectedOrigins() {
        return List.of(
                "https://unlisted.example.test",
                "https://attacker.example.test",
                "http://localhost:5174");
    }

    private HttpResponse<String> options(String path, String origin) throws Exception {
        return options(path, origin, HttpMethod.GET);
    }

    private HttpResponse<String> options(
            String path,
            String origin,
            HttpMethod requestedMethod
    ) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .header(HttpHeaders.ORIGIN, origin)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, requestedMethod.name())
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

class CrossSiteSessionCookieConfigurationTest {

    @Test
    void resolvesSecureSameSiteNoneCookieEnvironmentVariables() throws Exception {
        Properties applicationProperties = PropertiesLoaderUtils.loadProperties(
                new FileSystemResource("src/main/resources/application.properties"));
        MutablePropertySources propertySources = new MutablePropertySources();
        propertySources.addFirst(new MapPropertySource(
                "sessionCookieEnvironment",
                Map.of(
                        "SESSION_COOKIE_SAME_SITE", "none",
                        "SESSION_COOKIE_SECURE", "true")));
        PropertySourcesPropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);

        assertThat(resolver.resolveRequiredPlaceholders(applicationProperties.getProperty(
                "server.servlet.session.cookie.same-site"))).isEqualTo("none");
        assertThat(resolver.resolveRequiredPlaceholders(applicationProperties.getProperty(
                "server.servlet.session.cookie.secure"))).isEqualTo("true");
    }
}
