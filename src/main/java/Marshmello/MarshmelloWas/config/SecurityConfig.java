package Marshmello.MarshmelloWas.config;

import java.time.Duration;
import java.util.function.Consumer;

import Marshmello.MarshmelloWas.auth.ModelGateAuthorizationManager;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
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

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository,
            AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository,
            ModelGateAuthorizationManager modelGateAuthorizationManager,
            OidcSecurityProperties oidcProperties
    ) throws Exception {
        OAuth2AuthorizationRequestResolver authorizationRequestResolver =
            authorizationRequestResolver(clientRegistrationRepository, oidcProperties);

        http
            .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers(
                            "/actuator/health",
                            "/api/csrf",
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
                .authorizationEndpoint(endpoint -> endpoint
                    .authorizationRequestRepository(authorizationRequestRepository)
                    .authorizationRequestResolver(authorizationRequestResolver)
                )
                .defaultSuccessUrl("/", true)
            )
            .oauth2Client(client -> client
                .authorizedClientRepository(authorizedClientRepository)
            )
            .sessionManagement(session -> session
                .sessionFixation(fixation -> fixation.migrateSession())
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/")
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
            OidcSecurityProperties oidcProperties
    ) {
        DefaultOAuth2AuthorizationRequestResolver resolver =
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

        resolver.setAuthorizationRequestCustomizer(customizer);
        return resolver;
    }
}
