package Marshmello.MarshmelloWas.domain.report.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import Marshmello.MarshmelloWas.domain.report.dto.ReportResponse;
import Marshmello.MarshmelloWas.domain.report.service.ReportService;
import Marshmello.MarshmelloWas.global.exception.ApiException;
import Marshmello.MarshmelloWas.global.exception.ErrorCode;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ReportControllerHttpTest {

    private static final YearMonth REPORT_MONTH = YearMonth.of(2026, 7);
    private static final String CONTENT = "가".repeat(60);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @Test
    void createsReportForRequestedMonth() throws Exception {
        when(reportService.create(REPORT_MONTH)).thenReturn(response());

        mockMvc.perform(post("/api/reports")
                        .queryParam("month", "2026-07")
                        .with(oidcLogin())
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportId").value(10))
                .andExpect(jsonPath("$.yearMonth").value("2026-07"))
                .andExpect(jsonPath("$.content").value(CONTENT));

        verify(reportService).create(REPORT_MONTH);
    }

    @Test
    void returnsReportForRequestedMonth() throws Exception {
        when(reportService.get(REPORT_MONTH)).thenReturn(response());

        mockMvc.perform(get("/api/reports")
                        .queryParam("month", "2026-07")
                        .with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value(10))
                .andExpect(jsonPath("$.yearMonth").value("2026-07"))
                .andExpect(jsonPath("$.content").value(CONTENT));

        verify(reportService).get(REPORT_MONTH);
    }

    @Test
    void requiresValidYearMonth() throws Exception {
        mockMvc.perform(get("/api/reports").with(oidcLogin()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/reports")
                        .queryParam("month", "2026-7")
                        .with(oidcLogin()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reportService);
    }

    @Test
    void returnsConflictForDuplicateMonth() throws Exception {
        when(reportService.create(REPORT_MONTH))
                .thenThrow(new ApiException(ErrorCode.REPORT_ALREADY_EXISTS));

        mockMvc.perform(post("/api/reports")
                        .queryParam("month", "2026-07")
                        .with(oidcLogin())
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REPORT_ALREADY_EXISTS"));
    }

    @Test
    void returnsUnprocessableEntityForInvalidReportSource() throws Exception {
        when(reportService.create(REPORT_MONTH))
                .thenThrow(new ApiException(ErrorCode.REPORT_SOURCE_ERROR));

        mockMvc.perform(post("/api/reports")
                        .queryParam("month", "2026-07")
                        .with(oidcLogin())
                        .with(csrf()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("REPORT_SOURCE_ERROR"));
    }

    @Test
    void requiresAuthenticationAndCsrf() throws Exception {
        mockMvc.perform(get("/api/reports")
                        .queryParam("month", "2026-07"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/reports")
                        .queryParam("month", "2026-07")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/reports")
                        .queryParam("month", "2026-07")
                        .with(oidcLogin()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(reportService);
    }

    private ReportResponse response() {
        return new ReportResponse(10L, REPORT_MONTH, CONTENT);
    }
}
