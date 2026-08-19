package Marshmello.MarshmelloWas.domain.checkin.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import Marshmello.MarshmelloWas.domain.checkin.service.CheckInService;
import Marshmello.MarshmelloWas.global.exception.ApiException;
import Marshmello.MarshmelloWas.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;

@SpringBootTest
@AutoConfigureMockMvc
class CheckInDeleteControllerHttpTest {

    private static final String DATE = "2026-08-15";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CheckInService checkInService;

    @Test
    void deletesCurrentUsersCheckInWhenIdAndDateMatch() throws Exception {
        mockMvc.perform(delete("/api/check-ins/{checkInId}", 10)
                        .queryParam("date", DATE)
                        .with(oidcLogin())
                        .with(csrf()))
                .andExpect(status().isNoContent());
        verify(checkInService).delete(10L, LocalDate.parse(DATE));
    }

    @Test
    void returnsNotFoundWhenCheckInDoesNotExist() throws Exception {
        doThrow(new ApiException(ErrorCode.CHECK_IN_NOT_FOUND))
                .when(checkInService)
                .delete(10L, LocalDate.parse(DATE));

        mockMvc.perform(delete("/api/check-ins/{checkInId}", 10)
                        .queryParam("date", DATE)
                        .with(oidcLogin())
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHECK_IN_NOT_FOUND"));
    }

    @Test
    void rejectsMissingAndMalformedDatesBeforeCallingService() throws Exception {
        mockMvc.perform(delete("/api/check-ins/{checkInId}", 10)
                        .with(oidcLogin())
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mockMvc.perform(delete("/api/check-ins/{checkInId}", 10)
                        .queryParam("date", "15-08-2026")
                        .with(oidcLogin())
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(checkInService);
    }

    @Test
    void rejectsZeroAndNegativeIdsBeforeCallingService() throws Exception {
        mockMvc.perform(delete("/api/check-ins/{checkInId}", 0)
                        .queryParam("date", DATE)
                        .with(oidcLogin())
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mockMvc.perform(delete("/api/check-ins/{checkInId}", -1)
                        .queryParam("date", DATE)
                        .with(oidcLogin())
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(checkInService);
    }

    @Test
    void requiresAuthenticationAndCsrfBeforeCallingService() throws Exception {
        mockMvc.perform(delete("/api/check-ins/{checkInId}", 10)
                        .queryParam("date", DATE)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/check-ins/{checkInId}", 10)
                        .queryParam("date", DATE)
                        .with(oidcLogin()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(checkInService);
    }
}
