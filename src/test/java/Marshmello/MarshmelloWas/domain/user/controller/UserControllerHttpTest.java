package Marshmello.MarshmelloWas.domain.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import Marshmello.MarshmelloWas.domain.user.dto.UpdateUserProfileReqDto;
import Marshmello.MarshmelloWas.domain.user.dto.UserProfileResDto;
import Marshmello.MarshmelloWas.domain.user.service.UserService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void returnsCurrentUserProfile() throws Exception {
        when(userService.getProfile())
                .thenReturn(new UserProfileResDto(
                        "마시멜로", LocalDate.of(2026, 9, 1), false));

        mockMvc.perform(get("/api/user").with(user("subject")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("마시멜로"))
                .andExpect(jsonPath("$.expectedDeliveryDate").value("2026-09-01"))
                .andExpect(jsonPath("$.profileCompleted").value(false));

        verify(userService).getProfile();
    }

    @Test
    void updatesCurrentUserProfile() throws Exception {
        when(userService.updateProfile(any(UpdateUserProfileReqDto.class)))
                .thenReturn(new UserProfileResDto(
                        "새닉네임", LocalDate.of(2026, 10, 1), true));

        mockMvc.perform(patch("/api/user")
                        .with(user("subject"))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "새닉네임",
                                  "expectedDeliveryDate": "2026-10-01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("새닉네임"))
                .andExpect(jsonPath("$.expectedDeliveryDate").value("2026-10-01"))
                .andExpect(jsonPath("$.profileCompleted").value(true));

        verify(userService).updateProfile(
                new UpdateUserProfileReqDto("새닉네임", LocalDate.of(2026, 10, 1)));
    }

    @Test
    void rejectsInvalidProfileUpdateBeforeCallingService() throws Exception {
        mockMvc.perform(patch("/api/user")
                        .with(user("subject"))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": " ",
                                  "expectedDeliveryDate": "2026-10-01"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/user"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requiresCsrfTokenForProfileUpdate() throws Exception {
        mockMvc.perform(patch("/api/user")
                        .with(user("subject"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "새닉네임",
                                  "expectedDeliveryDate": "2026-10-01"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletesCurrentUserAndInvalidatesSession() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(delete("/api/user")
                        .session(session)
                        .with(user("subject"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(session.isInvalid()).isTrue();
        verify(userService).deleteUser();
    }

    @Test
    void requiresCsrfTokenForDeletion() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(delete("/api/user")
                        .session(session)
                        .with(user("subject")))
                .andExpect(status().isForbidden());

        assertThat(session.isInvalid()).isFalse();
        verifyNoInteractions(userService);
    }
}
