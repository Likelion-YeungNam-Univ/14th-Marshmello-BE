package Marshmello.MarshmelloWas.global.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String allowedOrigins;

    public CorsConfig(@Value("${app.cors.allowed-origins:http://localhost:5173}") String allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(Arrays.stream(allowedOrigins.split(",", -1))
                        .map(String::trim)
                        .filter(origin -> !origin.isEmpty())
                        .toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "X-CSRF-TOKEN")
                .allowCredentials(true);

        registry.addMapping("/logout")
                .allowedOrigins(allowedOrigins.split(",", -1))
                .allowedMethods("POST", "OPTIONS")
                .allowedHeaders("Content-Type", "X-CSRF-TOKEN")
                .allowCredentials(true);
    }
}
