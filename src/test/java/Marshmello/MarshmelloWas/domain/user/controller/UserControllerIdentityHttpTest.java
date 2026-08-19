package Marshmello.MarshmelloWas.domain.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import Marshmello.MarshmelloWas.domain.auth.entity.SocialAccount;
import Marshmello.MarshmelloWas.domain.auth.entity.SocialAccountId;
import Marshmello.MarshmelloWas.domain.auth.repository.SocialAccountRepository;
import Marshmello.MarshmelloWas.domain.auth.service.OidcUserProvisioningService;
import Marshmello.MarshmelloWas.domain.user.entity.User;
import Marshmello.MarshmelloWas.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerIdentityHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Autowired
    private OidcUserProvisioningService provisioningService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void returnsPendingProfileAfterFirstOidcLogin() throws Exception {
        provisioningService.provision("test", "new-provider-user");

        mockMvc.perform(get("/api/user")
                        .with(oidcLogin().idToken(token -> token.subject("new-provider-user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("마시멜로"))
                .andExpect(jsonPath("$.expectedDeliveryDate").doesNotExist())
                .andExpect(jsonPath("$.profileCompleted").value(false));
    }

    @Test
    void completesPendingProfileAfterProfileUpdate() throws Exception {
        provisioningService.provision("test", "onboarding-user");

        mockMvc.perform(patch("/api/user")
                        .with(oidcLogin().idToken(token -> token.subject("onboarding-user")))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "완료사용자",
                                  "expectedDeliveryDate": "2026-11-01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("완료사용자"))
                .andExpect(jsonPath("$.expectedDeliveryDate").value("2026-11-01"))
                .andExpect(jsonPath("$.profileCompleted").value(true));

        mockMvc.perform(get("/api/user")
                        .with(oidcLogin().idToken(token -> token.subject("onboarding-user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileCompleted").value(true));
    }

    @Test
    void resolvesOidcIdentityToCurrentUserProfile() throws Exception {
        LocalDate expectedDeliveryDate = LocalDate.of(2026, 10, 1);
        User user = userRepository.save(new User("마시멜로", expectedDeliveryDate));
        socialAccountRepository.save(new SocialAccount(
                new SocialAccountId("test", "provider-user-1"),
                user.getUserId()));

        mockMvc.perform(get("/api/user")
                        .with(oidcLogin().idToken(token -> token.subject("provider-user-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("마시멜로"))
                .andExpect(jsonPath("$.expectedDeliveryDate").value("2026-10-01"))
                .andExpect(jsonPath("$.profileCompleted").value(true));
    }

    @Test
    void returnsNotFoundWhenOidcIdentityHasNoUser() throws Exception {
        mockMvc.perform(get("/api/user")
                        .with(oidcLogin().idToken(token -> token.subject("unknown-user"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void doesNotResolveSameSubjectFromDifferentProvider() throws Exception {
        User user = userRepository.save(new User("마시멜로", null));
        socialAccountRepository.save(new SocialAccount(
                new SocialAccountId("other-provider", "shared-subject"),
                user.getUserId()));

        mockMvc.perform(get("/api/user")
                        .with(oidcLogin().idToken(token -> token.subject("shared-subject"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void deletesCurrentUserAndCascadesSocialAccount() throws Exception {
        User user = userRepository.save(new User("탈퇴사용자", null));
        SocialAccountId accountId = new SocialAccountId("test", "delete-user");
        socialAccountRepository.save(new SocialAccount(accountId, user.getUserId()));

        mockMvc.perform(delete("/api/user")
                        .with(oidcLogin().idToken(token -> token.subject("delete-user")))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();
        assertThat(userRepository.findById(user.getUserId()))
                .isEmpty();
        assertThat(socialAccountRepository.findById(accountId))
                .isEmpty();
    }
}
