package Marshmello.MarshmelloWas.domain.checkin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInCreateRequest;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInResponse;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInSummaryResponse;
import Marshmello.MarshmelloWas.domain.checkin.service.CheckInQueryService;
import Marshmello.MarshmelloWas.domain.checkin.service.CheckInService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CheckInControllerHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CheckInService checkInService;

    @MockitoBean
    private CheckInQueryService checkInQueryService;

    @Test
    void createsCheckInWithoutAcceptingUserIdOrDate() throws Exception {
        when(checkInService.create(any(CheckInCreateRequest.class))).thenReturn(new CheckInResponse(
                10L, 20L, true, LocalDate.of(2026, 8, 15), null, (short) 2, List.of()));

        mockMvc.perform(post("/api/check-ins")
                        .with(oidcLogin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "imageId": 20,
                                  "achieved": true,
                                  "diary": null,
                                  "emotion": 2,
                                  "bodyDiaries": []
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.checkInId").value(10))
                .andExpect(jsonPath("$.imageId").value(20))
                .andExpect(jsonPath("$.checkInDate").value("2026-08-15"));
    }

    @Test
    void rejectsEmotionOutsideOneToFour() throws Exception {
        mockMvc.perform(post("/api/check-ins")
                        .with(oidcLogin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "imageId": 20,
                                  "achieved": false,
                                  "emotion": 5,
                                  "bodyDiaries": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(checkInService);
    }

    @Test
    void returnsCheckInsForTheRequestedDate() throws Exception {
        LocalDate date = LocalDate.of(2026, 8, 15);
        when(checkInQueryService.getByDate(date)).thenReturn(List.of(new CheckInSummaryResponse(
                10L, 20L, date, true, (short) 2)));

        mockMvc.perform(get("/api/check-ins")
                        .queryParam("date", "2026-08-15")
                        .with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].checkInId").value(10))
                .andExpect(jsonPath("$[0].imageId").value(20));
    }

    @Test
    void requiresAValidIsoDateForCheckInLookup() throws Exception {
        mockMvc.perform(get("/api/check-ins").with(oidcLogin()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/check-ins")
                        .queryParam("date", "15-08-2026")
                        .with(oidcLogin()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(checkInQueryService);
    }

    @Test
    void requiresAuthenticationAndCsrf() throws Exception {
        String body = """
                {"imageId":20,"achieved":false,"emotion":1,"bodyDiaries":[]}
                """;
        mockMvc.perform(post("/api/check-ins")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/check-ins")
                        .with(oidcLogin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
        verifyNoInteractions(checkInService);
    }
}
