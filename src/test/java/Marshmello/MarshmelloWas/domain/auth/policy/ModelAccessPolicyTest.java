package Marshmello.MarshmelloWas.domain.auth.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

import Marshmello.MarshmelloWas.domain.auth.model.AuthenticatedPrincipal;
import Marshmello.MarshmelloWas.domain.auth.model.ModelAuthorizationRules;

class ModelAccessPolicyTest {

    @Test
    void authorizesAllowlistedSubjectBeforeEmailVerification() {
        ModelAccessPolicy policy = policy(List.of(), List.of(), List.of("subject-1"), true);

        boolean authorized = policy.isAllowed(
                new AuthenticatedPrincipal("subject-1", "user@example.com", false));

        assertThat(authorized).isTrue();
    }

    @Test
    void authorizesNormalizedEmail() {
        ModelAccessPolicy policy = policy(List.of(" USER@EXAMPLE.COM "), List.of(), List.of(), true);

        boolean authorized = policy.isAllowed(
                new AuthenticatedPrincipal("subject-1", "user@example.com", true));

        assertThat(authorized).isTrue();
    }

    @Test
    void authorizesNormalizedEmailDomain() {
        ModelAccessPolicy policy = policy(List.of(), List.of(" TRUSTED.EXAMPLE "), List.of(), true);

        boolean authorized = policy.isAllowed(
                new AuthenticatedPrincipal("subject-1", "user@trusted.example", true));

        assertThat(authorized).isTrue();
    }

    @Test
    void rejectsUnverifiedEmailWhenVerificationIsRequired() {
        ModelAccessPolicy policy = policy(List.of("user@example.com"), List.of(), List.of(), true);

        boolean authorized = policy.isAllowed(
                new AuthenticatedPrincipal("subject-1", "user@example.com", false));

        assertThat(authorized).isFalse();
    }

    @Test
    void permitsUnverifiedEmailWhenVerificationIsOptional() {
        ModelAccessPolicy policy = policy(List.of("user@example.com"), List.of(), List.of(), false);

        boolean authorized = policy.isAllowed(
                new AuthenticatedPrincipal("subject-1", "user@example.com", false));

        assertThat(authorized).isTrue();
    }

    @Test
    void failsClosedForMissingMalformedOrUnmatchedIdentity() {
        ModelAccessPolicy policy = policy(List.of(), List.of("trusted.example"), List.of(), true);

        assertThat(policy.isAllowed(new AuthenticatedPrincipal("subject-1", null, false))).isFalse();
        assertThat(policy.isAllowed(new AuthenticatedPrincipal("subject-1", "invalid-email", true))).isFalse();
        assertThat(policy.isAllowed(new AuthenticatedPrincipal("subject-1", "user@other.example", true))).isFalse();
    }

    private static ModelAccessPolicy policy(
            List<String> emails,
            List<String> domains,
            List<String> subjects,
            boolean requireVerifiedEmail) {
        return new ModelAccessPolicy(
                new ModelAuthorizationRules(emails, domains, subjects, requireVerifiedEmail));
    }
}
