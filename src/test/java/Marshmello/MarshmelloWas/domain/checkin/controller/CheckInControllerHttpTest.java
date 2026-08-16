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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInCreateRequest;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInEmotionResponse;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInResponse;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInSummaryResponse;
import Marshmello.MarshmelloWas.domain.checkin.dto.MonthlyCheckInCountResponse;
import Marshmello.MarshmelloWas.domain.checkin.dto.MostFrequentBodyRegionResponse;
import Marshmello.MarshmelloWas.domain.checkin.service.CheckInQueryService;
import Marshmello.MarshmelloWas.domain.checkin.service.CheckInService;
import java.time.LocalDate;
import java.time.YearMonth;
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
    void returnsMonthlyEmotionsAsAnOrderedList() throws Exception {
        when(checkInQueryService.getEmotionsByMonth(YearMonth.of(2026, 8)))
                .thenReturn(List.of(
                        new CheckInEmotionResponse(LocalDate.of(2026, 8, 2), (short) 2),
                        new CheckInEmotionResponse(LocalDate.of(2026, 8, 20), (short) 4)));

        mockMvc.perform(get("/api/check-ins/emotions")
                        .queryParam("month", "2026-08")
                        .with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date").value("2026-08-02"))
                .andExpect(jsonPath("$[0].emotion").value(2))
                .andExpect(jsonPath("$[1].date").value("2026-08-20"))
                .andExpect(jsonPath("$[1].emotion").value(4));
    }

    @Test
    void requiresAValidYearMonthForEmotionLookup() throws Exception {
        mockMvc.perform(get("/api/check-ins/emotions").with(oidcLogin()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/check-ins/emotions")
                        .queryParam("month", "2026-8")
                        .with(oidcLogin()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(checkInQueryService);
    }

    @Test
    void returnsMonthlyCheckInCount() throws Exception {
        when(checkInQueryService.getMonthlyCount(YearMonth.of(2026, 8)))
                .thenReturn(new MonthlyCheckInCountResponse(3, 2));

        mockMvc.perform(get("/api/check-ins/count")
                        .queryParam("month", "2026-08")
                        .with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3))
                .andExpect(jsonPath("$.achievedCount").value(2));
    }

    @Test
    void requiresAValidYearMonthForMonthlyCount() throws Exception {
        mockMvc.perform(get("/api/check-ins/count").with(oidcLogin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mockMvc.perform(get("/api/check-ins/count")
                        .queryParam("month", "2026-8")
                        .with(oidcLogin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(checkInQueryService);
    }

    @Test
    void requiresAuthenticationForMonthlyCount() throws Exception {
        mockMvc.perform(get("/api/check-ins/count")
                        .queryParam("month", "2026-08"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(checkInQueryService);
    }

    @Test
    void returnsMostFrequentMonthlyBodyDiaryRegionWithoutItsCount() throws Exception {
        when(checkInQueryService.getMostFrequentBodyRegion(YearMonth.of(2026, 8)))
                .thenReturn(new MostFrequentBodyRegionResponse((short) 2));

        mockMvc.perform(get("/api/check-ins/body-diaries/top-region")
                        .queryParam("month", "2026-08")
                        .with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bodyRegion").value(2))
                .andExpect(jsonPath("$.count").doesNotExist());
    }

    @Test
    void returnsAnExplicitNullBodyRegionWhenTheMonthHasNoBodyDiaries() throws Exception {
        when(checkInQueryService.getMostFrequentBodyRegion(YearMonth.of(2026, 8)))
                .thenReturn(new MostFrequentBodyRegionResponse(null));

        mockMvc.perform(get("/api/check-ins/body-diaries/top-region")
                        .queryParam("month", "2026-08")
                        .with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"bodyRegion\":null}"))
                .andExpect(jsonPath("$.count").doesNotExist());
    }

    @Test
    void requiresAValidYearMonthForTopBodyDiaryRegionLookup() throws Exception {
        mockMvc.perform(get("/api/check-ins/body-diaries/top-region").with(oidcLogin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mockMvc.perform(get("/api/check-ins/body-diaries/top-region")
                        .queryParam("month", "2026-8")
                        .with(oidcLogin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(checkInQueryService);
    }

    @Test
    void requiresAuthenticationForTopBodyDiaryRegionLookup() throws Exception {
        mockMvc.perform(get("/api/check-ins/body-diaries/top-region")
                        .queryParam("month", "2026-08"))
                .andExpect(status().isUnauthorized());
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
