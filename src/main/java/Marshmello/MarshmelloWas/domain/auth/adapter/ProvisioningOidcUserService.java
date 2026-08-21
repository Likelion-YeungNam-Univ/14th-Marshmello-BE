package Marshmello.MarshmelloWas.domain.auth.adapter;

import Marshmello.MarshmelloWas.domain.auth.service.OidcUserProvisioningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

@Component
public class ProvisioningOidcUserService
        implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final OidcUserProvisioningService provisioningService;
    private final OAuth2UserService<OidcUserRequest, OidcUser> delegate;

    @Autowired
    public ProvisioningOidcUserService(OidcUserProvisioningService provisioningService) {
        this(provisioningService, new OidcUserService());
    }

    ProvisioningOidcUserService(
            OidcUserProvisioningService provisioningService,
            OAuth2UserService<OidcUserRequest, OidcUser> delegate) {
        this.provisioningService = provisioningService;
        this.delegate = delegate;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = delegate.loadUser(userRequest);
        provisioningService.provision(
                userRequest.getClientRegistration().getRegistrationId(),
                oidcUser.getSubject());
        return oidcUser;
    }
}
