package Marshmello.MarshmelloWas.domain.auth.model;

public record AuthenticatedPrincipal(
        String subject,
        String email,
        boolean emailVerified
) {
}
