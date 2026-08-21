package Marshmello.MarshmelloWas.domain.auth.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("authenticated", authentication != null && authentication.isAuthenticated());

        if (authentication != null && authentication.getPrincipal() instanceof OidcUser user) {
            response.put("protocol", "oidc");
            response.put("subject", user.getSubject());
            response.put("email", user.getEmail());
            response.put("emailVerified", user.getEmailVerified());
            response.put("issuer", user.getIssuer() == null ? null : user.getIssuer().toString());
            response.put("audience", user.getAudience());
            response.put("authorities", authentication.getAuthorities().stream()
                .map(Object::toString)
                .sorted()
                .toList());
            return response;
        }

        response.put("protocol", "unknown");
        return response;
    }

    @GetMapping("/model-gate")
    public Map<String, Object> modelGate(Authentication authentication) {
        return Map.of(
            "authorized", true,
            "message", "MODEL_GATE_PASSED",
            "principal", authentication.getName()
        );
    }
}
