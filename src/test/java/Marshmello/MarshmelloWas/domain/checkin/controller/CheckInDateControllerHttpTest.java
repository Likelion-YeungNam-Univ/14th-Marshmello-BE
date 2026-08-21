package Marshmello.MarshmelloWas.domain.checkin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInCreateRequest;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInResponse;
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
class CheckInDateControllerHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CheckInService checkInService;

    @MockitoBean
    private CheckInQueryService checkInQueryService;

    @Test
    void createsCheckInForTheRequestedDate() throws Exception {
        LocalDate date = LocalDate.of(2026, 8, 10);
        when(checkInService.create(any(CheckInCreateRequest.class), any(LocalDate.class)))
                .thenReturn(new CheckInResponse(11L, 21L, false, date, null, (short) 1, List.of()));

        mockMvc.perform(post("/api/check-ins")
                        .queryParam("date", "2026-08-10")
                        .with(oidcLogin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "imageId": 21,
                                  "achieved": false,
                                  "diary": null,
                                  "emotion": 1,
                                  "bodyDiaries": []
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.checkInId").value(11))
                .andExpect(jsonPath("$.checkInDate").value("2026-08-10"));
    }

    @Test
    void requiresAValidIsoDateForDateBasedCheckInCreation() throws Exception {
        mockMvc.perform(post("/api/check-ins")
                        .queryParam("date", "10-08-2026")
                        .with(oidcLogin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"imageId":21,"achieved":false,"emotion":1,"bodyDiaries":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(checkInService);
    }
}
