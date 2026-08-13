package Marshmello.MarshmelloWas.domain.user.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import Marshmello.MarshmelloWas.domain.auth.entity.SocialAccount;
import Marshmello.MarshmelloWas.domain.auth.entity.SocialAccountId;
import Marshmello.MarshmelloWas.domain.auth.repository.SocialAccountRepository;
import Marshmello.MarshmelloWas.domain.user.entity.User;
import Marshmello.MarshmelloWas.domain.user.repository.UserRepository;
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
                .andExpect(jsonPath("$.expectedDeliveryDate").value("2026-10-01"));
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
}
