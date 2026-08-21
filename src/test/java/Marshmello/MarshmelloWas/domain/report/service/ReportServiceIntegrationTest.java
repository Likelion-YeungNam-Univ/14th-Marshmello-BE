package Marshmello.MarshmelloWas.domain.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import Marshmello.MarshmelloWas.domain.auth.port.CurrentUserIdProvider;
import Marshmello.MarshmelloWas.domain.checkin.entity.CheckIn;
import Marshmello.MarshmelloWas.domain.checkin.entity.Image;
import Marshmello.MarshmelloWas.domain.checkin.entity.ImageAnalysis;
import Marshmello.MarshmelloWas.domain.checkin.repository.BodyDiaryRepository;
import Marshmello.MarshmelloWas.domain.checkin.repository.CheckInRepository;
import Marshmello.MarshmelloWas.domain.checkin.repository.ImageAnalysisRepository;
import Marshmello.MarshmelloWas.domain.checkin.repository.ImageRepository;
import Marshmello.MarshmelloWas.domain.report.dto.ReportGenerationRequest;
import Marshmello.MarshmelloWas.domain.report.dto.ReportResponse;
import Marshmello.MarshmelloWas.domain.report.dto.ReportTrendPoint;
import Marshmello.MarshmelloWas.domain.report.entity.Report;
import Marshmello.MarshmelloWas.domain.report.port.ReportGenerator;
import Marshmello.MarshmelloWas.domain.report.port.ReportGenerator.GeneratedContent;
import Marshmello.MarshmelloWas.domain.report.port.ReportGenerator.GenerationException;
import Marshmello.MarshmelloWas.domain.report.repository.ReportRepository;
import Marshmello.MarshmelloWas.domain.user.entity.User;
import Marshmello.MarshmelloWas.domain.user.repository.UserRepository;
import Marshmello.MarshmelloWas.global.exception.ApiException;
import Marshmello.MarshmelloWas.global.exception.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@SpringBootTest
class ReportServiceIntegrationTest {

    private static final YearMonth REPORT_MONTH = YearMonth.of(2026, 7);
    private static final String GENERATED_CONTENT = "가".repeat(60);

    @Autowired
    private CheckInRepository checkInRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ImageAnalysisRepository imageAnalysisRepository;

    @Autowired
    private BodyDiaryRepository bodyDiaryRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final AtomicLong currentUserId = new AtomicLong();
    private final ReportGenerator generator = mock(ReportGenerator.class);
    private ReportService reportService;

    @BeforeEach
    void setUp() {
        CurrentUserIdProvider userIdProvider = currentUserId::get;
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-16T03:00:00Z"),
                ZoneId.of("Asia/Seoul"));
        reportService = new ReportService(
                checkInRepository,
                imageRepository,
                imageAnalysisRepository,
                reportRepository,
                userIdProvider,
                generator,
                transactionManager,
                clock);
    }

    @AfterEach
    void cleanUp() {
        reportRepository.deleteAll();
        bodyDiaryRepository.deleteAll();
        imageAnalysisRepository.deleteAll();
        imageRepository.deleteAll();
        checkInRepository.deleteAll();
        userRepository.deleteAll();
        reset(generator);
    }

    @Test
    void createsMonthlyReportFromAnalyzedCheckInsAndExcludesMissingAnalysis() {
        long userId = saveUser("report-owner");
        saveCheckIn(userId, LocalDate.of(2026, 7, 20), (short) 7);
        saveCheckIn(userId, LocalDate.of(2026, 7, 2), (short) 3);
        saveCheckIn(userId, LocalDate.of(2026, 7, 10), null);
        saveCheckIn(userId, LocalDate.of(2026, 6, 30), (short) 1);
        when(generator.generate(any())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return new GeneratedContent(GENERATED_CONTENT);
        });

        ReportResponse response = reportService.create(REPORT_MONTH);

        assertThat(response.yearMonth()).isEqualTo(REPORT_MONTH);
        assertThat(response.content()).isEqualTo(GENERATED_CONTENT);
        verify(generator).generate(new ReportGenerationRequest(java.util.List.of(
                new ReportTrendPoint(LocalDate.of(2026, 7, 2), (short) 3),
                new ReportTrendPoint(LocalDate.of(2026, 7, 20), (short) 7))));
        assertThat(reportRepository.findByUserIdAndReportMonth(userId, REPORT_MONTH.atDay(1)))
                .isPresent();
    }

    @Test
    void rejectsCurrentOrFutureMonthBeforeReadingSources() {
        currentUserId.set(saveUser("period-owner"));

        assertApiError(() -> reportService.create(YearMonth.of(2026, 8)), ErrorCode.INVALID_REPORT_PERIOD);
        assertApiError(() -> reportService.create(YearMonth.of(2026, 9)), ErrorCode.INVALID_REPORT_PERIOD);
        verifyNoInteractions(generator);
    }

    @Test
    void rejectsMonthWithoutCheckIns() {
        currentUserId.set(saveUser("empty-owner"));

        assertApiError(() -> reportService.create(REPORT_MONTH), ErrorCode.REPORT_SOURCE_EMPTY);
        verifyNoInteractions(generator);
    }

    @Test
    void rejectsMonthWithFewerThanTwoAnalyzedCheckIns() {
        long userId = saveUser("source-owner");
        saveCheckIn(userId, LocalDate.of(2026, 7, 2), (short) 3);
        saveCheckIn(userId, LocalDate.of(2026, 7, 10), null);

        assertApiError(() -> reportService.create(REPORT_MONTH), ErrorCode.REPORT_SOURCE_ERROR);
        verifyNoInteractions(generator);
    }

    @Test
    void rejectsDuplicateMonthWithoutCallingGeneratorAgain() {
        long userId = saveUser("duplicate-owner");
        saveCheckIn(userId, LocalDate.of(2026, 7, 2), (short) 3);
        saveCheckIn(userId, LocalDate.of(2026, 7, 20), (short) 7);
        when(generator.generate(any())).thenReturn(new GeneratedContent(GENERATED_CONTENT));
        reportService.create(REPORT_MONTH);

        assertApiError(() -> reportService.create(REPORT_MONTH), ErrorCode.REPORT_ALREADY_EXISTS);
        verify(generator).generate(any());
    }

    @Test
    void returnsOnlyCurrentUsersReportForRequestedMonth() {
        long ownerId = saveUser("get-owner");
        reportRepository.save(new Report(GENERATED_CONTENT, REPORT_MONTH, ownerId));

        ReportResponse response = reportService.get(REPORT_MONTH);

        assertThat(response.yearMonth()).isEqualTo(REPORT_MONTH);
        currentUserId.set(saveUser("get-other"));
        assertApiError(() -> reportService.get(REPORT_MONTH), ErrorCode.REPORT_NOT_FOUND);
    }

    @Test
    void mapsGeneratorFailureToApiError() {
        long userId = saveUser("generator-owner");
        saveCheckIn(userId, LocalDate.of(2026, 7, 2), (short) 3);
        saveCheckIn(userId, LocalDate.of(2026, 7, 20), (short) 7);
        when(generator.generate(any())).thenThrow(new GenerationException(
                GenerationException.Reason.TIMEOUT));

        assertApiError(() -> reportService.create(REPORT_MONTH), ErrorCode.AI_GENERATION_TIMEOUT);
        assertThat(reportRepository.count()).isZero();
    }

    private long saveUser(String nickname) {
        long userId = userRepository.save(new User(nickname, null)).getUserId();
        currentUserId.set(userId);
        return userId;
    }

    private void saveCheckIn(long userId, LocalDate date, Short score) {
        CheckIn checkIn = checkInRepository.save(new CheckIn(false, date, null, (short) 1, userId));
        Image image = new Image(userId, "test/report-" + date, "image/png", Instant.now());
        image.attachTo(checkIn, userId);
        imageRepository.save(image);
        if (score != null) {
            imageAnalysisRepository.save(new ImageAnalysis(image.id(), score));
        }
    }

    private static void assertApiError(Runnable action, ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(errorCode));
    }
}
