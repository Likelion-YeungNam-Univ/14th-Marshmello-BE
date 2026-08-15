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
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInTimelineItemResponse;
import Marshmello.MarshmelloWas.domain.checkin.service.CheckInQueryService;
import Marshmello.MarshmelloWas.domain.checkin.service.CheckInService;
import Marshmello.MarshmelloWas.global.web.PageResponse;
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
    void returnsTimelineWithImageIds() throws Exception {
        when(checkInQueryService.getTimeline(0, 20)).thenReturn(new PageResponse<>(
                List.of(new CheckInTimelineItemResponse(
                        10L, 20L, LocalDate.of(2026, 8, 15), true, (short) 2)),
                0, 20, 1, 1));

        mockMvc.perform(get("/api/check-ins").with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].checkInId").value(10))
                .andExpect(jsonPath("$.content[0].imageId").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));
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
