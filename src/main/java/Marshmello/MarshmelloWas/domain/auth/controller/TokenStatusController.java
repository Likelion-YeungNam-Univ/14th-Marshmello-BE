package Marshmello.MarshmelloWas.domain.auth.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TokenStatusController {

    private final OAuth2AuthorizedClientRepository authorizedClientRepository;

    public TokenStatusController(OAuth2AuthorizedClientRepository authorizedClientRepository) {
        this.authorizedClientRepository = authorizedClientRepository;
    }

    @GetMapping("/token-status")
    public Map<String, Object> tokenStatus(
            Authentication authentication,
            HttpServletRequest request
    ) {
        OAuth2AuthorizedClient client = authorizedClientRepository.loadAuthorizedClient(
            "oidc",
            authentication,
            request
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("registrationId", "oidc");

        if (client == null) {
            response.put("authorizedClientPresent", false);
            return response;
        }

        response.put("authorizedClientPresent", true);
        response.put("accessTokenType", client.getAccessToken().getTokenType().getValue());
        response.put("accessTokenScopes", client.getAccessToken().getScopes());
        response.put("accessTokenIssuedAt", client.getAccessToken().getIssuedAt());
        response.put("accessTokenExpiresAt", client.getAccessToken().getExpiresAt());
        response.put("refreshTokenPresent", client.getRefreshToken() != null);

        // Raw access_token / refresh_token values are deliberately never returned.
        return response;
    }
}
