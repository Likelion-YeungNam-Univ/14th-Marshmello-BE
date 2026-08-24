package Marshmello.MarshmelloWas.global.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class CorsAllowedOrigins {

    private final List<String> values;
    private final Set<String> loginOrigins;

    public CorsAllowedOrigins(@Value("${app.cors.allowed-origins}") String configuredOrigins) {
        this.values = Arrays.stream(configuredOrigins.split(",", -1))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .distinct()
                .toList();
        this.loginOrigins = values.stream()
                .map(CorsAllowedOrigins::originOf)
                .flatMap(Optional::stream)
                .collect(Collectors.toUnmodifiableSet());
    }

    public List<String> values() {
        return values;
    }

    public Optional<String> match(String candidate) {
        return originOf(candidate).filter(loginOrigins::contains);
    }

    private static Optional<String> originOf(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null
                    || host == null
                    || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                return Optional.empty();
            }
            return Optional.of(new URI(
                    scheme.toLowerCase(Locale.ROOT),
                    null,
                    host.toLowerCase(Locale.ROOT),
                    uri.getPort(),
                    null,
                    null,
                    null).toString());
        } catch (IllegalArgumentException | URISyntaxException ignored) {
            return Optional.empty();
        }
    }
}
