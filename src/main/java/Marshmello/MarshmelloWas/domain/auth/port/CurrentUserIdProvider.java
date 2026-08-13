package Marshmello.MarshmelloWas.domain.auth.port;

@FunctionalInterface
public interface CurrentUserIdProvider {

    long requireCurrentUserId();
}
