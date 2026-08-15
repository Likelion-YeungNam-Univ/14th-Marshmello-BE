package Marshmello.MarshmelloWas.domain.report.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import Marshmello.MarshmelloWas.domain.auth.port.CurrentUserIdProvider;
import Marshmello.MarshmelloWas.domain.checkin.entity.CheckIn;
import Marshmello.MarshmelloWas.domain.checkin.entity.Image;
import Marshmello.MarshmelloWas.domain.checkin.entity.ImageAnalysis;
import Marshmello.MarshmelloWas.domain.checkin.repository.CheckInRepository;
import Marshmello.MarshmelloWas.domain.checkin.repository.ImageAnalysisRepository;
import Marshmello.MarshmelloWas.domain.checkin.repository.ImageRepository;
import Marshmello.MarshmelloWas.domain.report.port.ReportGenerator;
import Marshmello.MarshmelloWas.domain.report.port.ReportGenerator.GeneratedContent;
import Marshmello.MarshmelloWas.domain.report.repository.ReportRepository;
import Marshmello.MarshmelloWas.domain.user.entity.User;
import Marshmello.MarshmelloWas.domain.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ReportControllerIdentityHttpTest {

    private static final String CONTENT = "가".repeat(60);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Clock clock;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CheckInRepository checkInRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ImageAnalysisRepository imageAnalysisRepository;

    @Autowired
    private ReportRepository reportRepository;

    @MockitoBean(name = "oidcCurrentUserIdProvider")
    private CurrentUserIdProvider currentUserIdProvider;

    @MockitoBean
    private ReportGenerator reportGenerator;

    @AfterEach
    void cleanUp() {
        reportRepository.deleteAll();
        imageAnalysisRepository.deleteAll();
        imageRepository.deleteAll();
        checkInRepository.deleteAll();
        userRepository.deleteAll();
        reset(currentUserIdProvider, reportGenerator);
    }

    @Test
    void createsReadsAndRejectsDuplicateMonthlyReportForCurrentUser() throws Exception {
        User user = userRepository.save(new User("report-owner", null));
        when(currentUserIdProvider.requireCurrentUserId()).thenReturn(user.getUserId());
        YearMonth month = YearMonth.now(clock).minusMonths(1);
        saveAnalyzedCheckIn(user.getUserId(), month.atDay(2), (short) 3);
        saveAnalyzedCheckIn(user.getUserId(), month.atDay(20), (short) 7);
        when(reportGenerator.generate(any())).thenReturn(new GeneratedContent(CONTENT));

        mockMvc.perform(post("/api/reports")
                        .queryParam("month", month.toString())
                        .with(oidcLogin())
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.yearMonth").value(month.toString()))
                .andExpect(jsonPath("$.content").value(CONTENT));

        mockMvc.perform(get("/api/reports")
                        .queryParam("month", month.toString())
                        .with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yearMonth").value(month.toString()))
                .andExpect(jsonPath("$.content").value(CONTENT));

        mockMvc.perform(post("/api/reports")
                        .queryParam("month", month.toString())
                        .with(oidcLogin())
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REPORT_ALREADY_EXISTS"));

        verify(reportGenerator, times(1)).generate(any());
    }

    private void saveAnalyzedCheckIn(long userId, LocalDate date, short score) {
        CheckIn checkIn = checkInRepository.save(new CheckIn(false, date, null, (short) 1, userId));
        Image image = new Image(
                userId,
                "test/report-http-" + date,
                "image/png",
                Instant.now(clock));
        image.attachTo(checkIn, userId);
        imageRepository.save(image);
        imageAnalysisRepository.save(new ImageAnalysis(image.id(), score));
    }
}
