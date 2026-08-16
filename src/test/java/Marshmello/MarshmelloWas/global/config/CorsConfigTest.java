package Marshmello.MarshmelloWas.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@SpringBootTest
@AutoConfigureMockMvc
@Import({CorsConfigTest.CorsProbeController.class, CorsConfigTest.CorsProbeSecurityConfiguration.class})
class CorsConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registersDefaultCorsPolicyForApiPaths() {
        InspectableCorsRegistry registry = new InspectableCorsRegistry();
        new CorsConfig("http://localhost:5173").addCorsMappings(registry);

        Map<String, CorsConfiguration> registrations = registry.registrations();
        CorsConfiguration registration = registrations.get("/api/**");

        assertThat(registrations).containsOnlyKeys("/api/**");
        assertThat(registration.getAllowedOrigins()).containsExactly("http://localhost:5173");
        assertThat(registration.getAllowedMethods()).containsExactly(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name());
        assertThat(registration.getAllowedHeaders()).containsExactly("Content-Type", "X-CSRF-TOKEN");
        assertThat(registration.getAllowCredentials()).isTrue();
    }

    @Test
    void registersTwoCommaSeparatedOriginsInOrder() {
        InspectableCorsRegistry registry = new InspectableCorsRegistry();
        new CorsConfig("http://localhost:5173,https://staging.example.test").addCorsMappings(registry);

        assertThat(registry.registrations().get("/api/**").getAllowedOrigins())
                .containsExactly("http://localhost:5173", "https://staging.example.test");
    }

    @Test
    void trimsOriginsAndDiscardsBlankValues() {
        InspectableCorsRegistry registry = new InspectableCorsRegistry();
        new CorsConfig(" , http://localhost:5173 , , https://staging.example.test , ").addCorsMappings(registry);

        assertThat(registry.registrations().get("/api/**").getAllowedOrigins())
                .containsExactly("http://localhost:5173", "https://staging.example.test");

        InspectableCorsRegistry blankRegistry = new InspectableCorsRegistry();
        new CorsConfig("   ").addCorsMappings(blankRegistry);

        assertThat(blankRegistry.registrations().get("/api/**").getAllowedOrigins()).isEmpty();
    }

    @Test
    void allowsConfiguredOriginPreflightForApiPaths() throws Exception {
        mockMvc.perform(options("/api/cors-probe")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name())
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void rejectsArbitraryOriginPreflightWithoutAllowOriginHeader() throws Exception {
        mockMvc.perform(options("/api/cors-probe")
                        .header(HttpHeaders.ORIGIN, "https://example.invalid")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name()))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void rejectsLookalikeLocalhostOriginWithoutAllowOriginHeader() throws Exception {
        mockMvc.perform(options("/api/cors-probe")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5174")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name()))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void rejectsPreflightOutsideApiPathsWithoutCrossOriginGrant() throws Exception {
        mockMvc.perform(options("/cors-probe")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name()))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @RestController
    static class CorsProbeController {

        @GetMapping({"/api/cors-probe", "/cors-probe"})
        void probe() {
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CorsProbeSecurityConfiguration {

        @Bean
        @Order(0)
        SecurityFilterChain corsProbeSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                    .securityMatcher("/api/cors-probe", "/cors-probe")
                    .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                    .csrf(AbstractHttpConfigurer::disable)
                    .cors(Customizer.withDefaults());
            return http.build();
        }
    }

    private static final class InspectableCorsRegistry extends CorsRegistry {

        private Map<String, CorsConfiguration> registrations() {
            return getCorsConfigurations();
        }
    }
}
