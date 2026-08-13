package Marshmello.MarshmelloWas.infrastructure.auth.security;

import Marshmello.MarshmelloWas.domain.auth.port.CurrentUserIdProvider;
import Marshmello.MarshmelloWas.global.exception.AuthenticationRequiredException;
import org.springframework.context.annotation.Fallback;
import org.springframework.stereotype.Component;

@Component
@Fallback
public class UnauthenticatedCurrentUserIdProvider implements CurrentUserIdProvider {

    @Override
    public long requireCurrentUserId() {
        throw new AuthenticationRequiredException();
    }
}
