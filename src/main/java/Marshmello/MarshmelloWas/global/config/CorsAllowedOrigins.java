package Marshmello.MarshmelloWas.global.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class CorsAllowedOrigins {

    private final List<String> values;

    public CorsAllowedOrigins(@Value("${app.cors.allowed-origins}") String configuredOrigins) {
        this.values = Arrays.stream(configuredOrigins.split(",", -1))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .distinct()
                .toList();
    }

    public List<String> values() {
        return values;
    }
}
