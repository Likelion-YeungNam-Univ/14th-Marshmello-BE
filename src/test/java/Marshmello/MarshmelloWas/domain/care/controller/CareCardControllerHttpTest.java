package Marshmello.MarshmelloWas.domain.care.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import Marshmello.MarshmelloWas.domain.care.dto.CareCardCreationResult;
import Marshmello.MarshmelloWas.domain.care.dto.CareCardResponse;
import Marshmello.MarshmelloWas.domain.care.service.CareCardService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CareCardControllerHttpTest {

    private static final long CHECK_IN_ID = 10L;
    private static final long CARE_CARD_ID = 20L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CareCardService careCardService;

    @Test
    void returnsCreatedForNewCareCard() throws Exception {
        when(careCardService.create(CHECK_IN_ID))
                .thenReturn(new CareCardCreationResult(response(), true));

        mockMvc.perform(post("/api/check-ins/{checkInId}/care-card", CHECK_IN_ID)
                        .with(user("subject"))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.careCardId").value(CARE_CARD_ID))
                .andExpect(jsonPath("$.checkInId").value(CHECK_IN_ID))
                .andExpect(jsonPath("$.category").value("카테고리"))
                .andExpect(jsonPath("$.source").value("출처"))
                .andExpect(jsonPath("$.createdDate").value("2026-08-14"));
    }

    @Test
    void returnsOkForExistingCareCard() throws Exception {
        when(careCardService.create(CHECK_IN_ID))
                .thenReturn(new CareCardCreationResult(response(), false));

        mockMvc.perform(post("/api/check-ins/{checkInId}/care-card", CHECK_IN_ID)
                        .with(user("subject"))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void returnsCareCardForOwnedCheckIn() throws Exception {
        when(careCardService.getByCheckInId(CHECK_IN_ID)).thenReturn(response());

        mockMvc.perform(get("/api/check-ins/{checkInId}/care-card", CHECK_IN_ID)
                        .with(user("subject")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actionName").value("행동"))
                .andExpect(jsonPath("$.actionReason").value("이유"));
    }

    @Test
    void updatesFeedback() throws Exception {
        mockMvc.perform(patch("/api/care-cards/{careCardId}/feedback", CARE_CARD_ID)
                        .with(user("subject"))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"helpfulnessScore": 5}
                                """))
                .andExpect(status().isNoContent());

        verify(careCardService).updateFeedback(CARE_CARD_ID, (short) 5);
    }

    @Test
    void rejectsInvalidFeedbackBeforeCallingService() throws Exception {
        mockMvc.perform(patch("/api/care-cards/{careCardId}/feedback", CARE_CARD_ID)
                        .with(user("subject"))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"helpfulnessScore": 0}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(careCardService);
    }

    @Test
    void requiresAuthenticationAndCsrfForCreation() throws Exception {
        mockMvc.perform(post("/api/check-ins/{checkInId}/care-card", CHECK_IN_ID)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/check-ins/{checkInId}/care-card", CHECK_IN_ID)
                        .with(user("subject")))
                .andExpect(status().isForbidden());
    }

    private CareCardResponse response() {
        return new CareCardResponse(
                CARE_CARD_ID,
                CHECK_IN_ID,
                "행동",
                "이유",
                "카테고리",
                "출처",
                LocalDate.of(2026, 8, 14));
    }
}
