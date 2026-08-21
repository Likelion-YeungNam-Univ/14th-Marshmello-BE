package Marshmello.MarshmelloWas.domain.auth.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import Marshmello.MarshmelloWas.domain.auth.service.OidcUserProvisioningService;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

class ProvisioningOidcUserServiceTest {

    @Test
    void provisionsLocalUserAfterLoadingOidcUser() {
        OidcUserProvisioningService provisioningService =
                mock(OidcUserProvisioningService.class);
        @SuppressWarnings("unchecked")
        OAuth2UserService<OidcUserRequest, OidcUser> delegate =
                mock(OAuth2UserService.class);
        OidcUserRequest request = mock(OidcUserRequest.class);
        ClientRegistration registration = mock(ClientRegistration.class);
        OidcUser oidcUser = mock(OidcUser.class);

        when(request.getClientRegistration()).thenReturn(registration);
        when(registration.getRegistrationId()).thenReturn("oidc");
        when(oidcUser.getSubject()).thenReturn("provider-user-1");
        when(delegate.loadUser(request)).thenReturn(oidcUser);

        ProvisioningOidcUserService userService =
                new ProvisioningOidcUserService(provisioningService, delegate);

        OidcUser result = userService.loadUser(request);

        assertThat(result).isSameAs(oidcUser);
        verify(provisioningService).provision("oidc", "provider-user-1");
    }
}
