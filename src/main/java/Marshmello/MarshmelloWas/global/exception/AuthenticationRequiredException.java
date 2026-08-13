package Marshmello.MarshmelloWas.global.exception;

public final class AuthenticationRequiredException extends ApiException {

    public AuthenticationRequiredException() {
        super(ErrorCode.AUTHENTICATION_REQUIRED);
    }
}
