package Marshmello.MarshmelloWas.infrastructure.auth.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import Marshmello.MarshmelloWas.domain.auth.adapter.ModelGateAuthorizationManager;
import Marshmello.MarshmelloWas.domain.auth.adapter.ProvisioningOidcUserService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.CompositeLogoutHandler;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.csrf.CsrfLogoutHandler;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@Configuration
public class SecurityConfig {

    private static final String LOGIN_SUCCESS_URL_ATTRIBUTE =
            SecurityConfig.class.getName() + ".loginSuccessUrl";

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository,
            AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository,
            CsrfTokenRepository csrfTokenRepository,
            ModelGateAuthorizationManager modelGateAuthorizationManager,
            ProvisioningOidcUserService provisioningOidcUserService,
            OidcSecurityProperties oidcProperties,
            @Value("${app.cors.allowed-origins:http://localhost:5173}") String allowedOrigins,
            @Value("${app.login-success-url:/}") String loginSuccessUrl
    ) throws Exception {
        Set<String> allowedLoginOrigins = parseOrigins(allowedOrigins);
        OAuth2AuthorizationRequestResolver authorizationRequestResolver =
            authorizationRequestResolver(clientRegistrationRepository, oidcProperties, allowedLoginOrigins);

        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository))
            .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers(
                            "/actuator/health",
                            "/api/csrf",
                            "/v3/api-docs/**",
                            "/swagger-ui/**",
                            "/swagger-ui.html",
                            "/oauth2/**",
                            "/login/**",
                            "/error"
                    ).permitAll()

                    .requestMatchers("/api/model/**")
                    .access(modelGateAuthorizationManager)

                    .requestMatchers("/api/**")
                    .authenticated()

                    .anyRequest()
                    .denyAll()
            )
            .oauth2Login(login -> login
                .authorizedClientRepository(authorizedClientRepository)
                .userInfoEndpoint(userInfo -> userInfo
                    .oidcUserService(provisioningOidcUserService)
                )
                .authorizationEndpoint(endpoint -> endpoint
                    .authorizationRequestRepository(authorizationRequestRepository)
                    .authorizationRequestResolver(authorizationRequestResolver)
                )
                .successHandler((request, response, authentication) -> {
                    var session = request.getSession(false);
                    String selectedLoginSuccessUrl = session == null
                            ? null
                            : (String) session.getAttribute(LOGIN_SUCCESS_URL_ATTRIBUTE);
                    if (session != null) {
                        session.removeAttribute(LOGIN_SUCCESS_URL_ATTRIBUTE);
                    }
                    response.sendRedirect(selectedLoginSuccessUrl == null
                            ? loginSuccessUrl
                            : selectedLoginSuccessUrl);
                })
            )
            .oauth2Client(client -> client
                .authorizedClientRepository(authorizedClientRepository)
            )
            .sessionManagement(session -> session
                .sessionFixation(fixation -> fixation.migrateSession())
            )
            .logout(logout -> logout
                .logoutSuccessHandler(
                    new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)
                )
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
            )
            .exceptionHandling(exceptions -> exceptions
                .defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    request -> request.getRequestURI().startsWith("/api/")
                )
            );

        // CSRF remains enabled. POST /logout and any future model POST must include the token.
        return http.build();
    }

    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        return new HttpSessionCsrfTokenRepository();
    }

    @Bean
    LogoutHandler localLogoutHandler(CsrfTokenRepository csrfTokenRepository) {
        return new CompositeLogoutHandler(
                new CsrfLogoutHandler(csrfTokenRepository),
                new SecurityContextLogoutHandler(),
                new CookieClearingLogoutHandler("JSESSIONID"));
    }

    @Bean
    AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository() {
        // Stores state, nonce-related request data, and PKCE verifier server-side between
        // authorization redirect and callback.
        return new HttpSessionOAuth2AuthorizationRequestRepository();
    }

    @Bean
    OAuth2AuthorizedClientRepository authorizedClientRepository() {
        // Access/refresh tokens are stored server-side in HttpSession, not in browser storage.
        return new HttpSessionOAuth2AuthorizedClientRepository();
    }

    @Bean
    OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository
    ) {
        OAuth2AuthorizedClientProvider provider = OAuth2AuthorizedClientProviderBuilder.builder()
            .authorizationCode()
            .refreshToken(refresh -> refresh.clockSkew(Duration.ofSeconds(60)))
            .build();

        DefaultOAuth2AuthorizedClientManager manager = new DefaultOAuth2AuthorizedClientManager(
            clientRegistrationRepository,
            authorizedClientRepository
        );
        manager.setAuthorizedClientProvider(provider);
        return manager;
    }

    private OAuth2AuthorizationRequestResolver authorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository,
            OidcSecurityProperties oidcProperties,
            Set<String> allowedLoginOrigins
    ) {
        DefaultOAuth2AuthorizationRequestResolver delegate =
            new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository,
                "/oauth2/authorization"
            );

        Consumer<OAuth2AuthorizationRequest.Builder> customizer =
            OAuth2AuthorizationRequestCustomizers.withPkce();

        if (oidcProperties.googleOfflineAccess()) {
            Consumer<OAuth2AuthorizationRequest.Builder> googleOffline = builder ->
                builder.additionalParameters(parameters -> {
                    parameters.put("access_type", "offline");
                    parameters.put("prompt", "consent");
                });
            customizer = customizer.andThen(googleOffline);
        }

        delegate.setAuthorizationRequestCustomizer(customizer);
        return new OriginAwareAuthorizationRequestResolver(delegate, allowedLoginOrigins);
    }

    private static Set<String> parseOrigins(String rawOrigins) {
        return Arrays.stream(rawOrigins.split(",", -1))
                .map(String::trim)
                .map(SecurityConfig::originOf)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String originOf(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null
                    || host == null
                    || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                return null;
            }
            return new URI(
                    scheme.toLowerCase(Locale.ROOT),
                    null,
                    host.toLowerCase(Locale.ROOT),
                    uri.getPort(),
                    null,
                    null,
                    null).toString();
        } catch (IllegalArgumentException | URISyntaxException ignored) {
            return null;
        }
    }

    private static final class OriginAwareAuthorizationRequestResolver
            implements OAuth2AuthorizationRequestResolver {

        private final OAuth2AuthorizationRequestResolver delegate;
        private final Set<String> allowedLoginOrigins;

        private OriginAwareAuthorizationRequestResolver(
                OAuth2AuthorizationRequestResolver delegate,
                Set<String> allowedLoginOrigins
        ) {
            this.delegate = delegate;
            this.allowedLoginOrigins = allowedLoginOrigins;
        }

        @Override
        public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
            return decorate(request, delegate.resolve(request));
        }

        @Override
        public OAuth2AuthorizationRequest resolve(
                HttpServletRequest request,
                String clientRegistrationId
        ) {
            return decorate(request, delegate.resolve(request, clientRegistrationId));
        }

        private OAuth2AuthorizationRequest decorate(
                HttpServletRequest request,
                OAuth2AuthorizationRequest authorizationRequest
        ) {
            if (authorizationRequest == null) {
                return null;
            }

            String loginSuccessUrl = findAllowedOrigin(request);
            if (loginSuccessUrl == null) {
                request.getSession().removeAttribute(LOGIN_SUCCESS_URL_ATTRIBUTE);
                return authorizationRequest;
            }

            request.getSession().setAttribute(LOGIN_SUCCESS_URL_ATTRIBUTE, loginSuccessUrl);
            return authorizationRequest;
        }

        private String findAllowedOrigin(HttpServletRequest request) {
            String[] candidates = {
                    request.getHeader("Origin"),
                    request.getHeader("Referer")
            };
            return Arrays.stream(candidates)
                    .map(SecurityConfig::originOf)
                    .filter(Objects::nonNull)
                    .filter(allowedLoginOrigins::contains)
                    .findFirst()
                    .orElse(null);
        }
    }
}
