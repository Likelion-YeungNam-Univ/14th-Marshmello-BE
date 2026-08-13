package Marshmello.MarshmelloWas.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@SpringBootTest
@AutoConfigureMockMvc
@Import({CorsConfigTest.CorsProbeController.class, ProbeSecurityTestConfig.class})
class CorsConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registersCredentialsOnlyCorsPolicyForEveryPath() {
        InspectableCorsRegistry registry = new InspectableCorsRegistry();
        new CorsConfig().addCorsMappings(registry);

        Map<String, CorsConfiguration> registrations = registry.registrations();
        CorsConfiguration registration = registrations.get("/**");

        assertThat(registrations).containsOnlyKeys("/**");
        assertThat(registration.getAllowedOrigins()).isEmpty();
        assertThat(registration.getAllowedMethods()).containsExactly(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name());
        assertThat(registration.getAllowCredentials()).isTrue();
    }

    @Test
    void rejectsArbitraryOriginPreflightWithoutAllowOriginHeader() throws Exception {
        mockMvc.perform(options("/cors-probe")
                        .header(HttpHeaders.ORIGIN, "https://example.invalid")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name()))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void leavesSameOriginRequestUsableWithoutCrossOriginGrant() throws Exception {
        mockMvc.perform(get("/cors-probe"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @RestController
    static class CorsProbeController {

        @GetMapping("/cors-probe")
        void probe() {
        }
    }

    private static final class InspectableCorsRegistry extends CorsRegistry {

        private Map<String, CorsConfiguration> registrations() {
            return getCorsConfigurations();
        }
    }
}
