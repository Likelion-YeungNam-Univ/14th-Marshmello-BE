package Marshmello.MarshmelloWas.global.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration(proxyBeanMethods = false)
public class ProbeSecurityTestConfig {

    @Bean
    @Order(0)
    SecurityFilterChain probeSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/cors-probe", "/error-probe/**")
            .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults());
        return http.build();
    }
}
