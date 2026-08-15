package Marshmello.MarshmelloWas.domain.report.service;

import Marshmello.MarshmelloWas.domain.auth.port.CurrentUserIdProvider;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInImageReference;
import Marshmello.MarshmelloWas.domain.checkin.entity.CheckIn;
import Marshmello.MarshmelloWas.domain.checkin.entity.ImageAnalysis;
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
import Marshmello.MarshmelloWas.global.exception.ApiException;
import Marshmello.MarshmelloWas.global.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ReportService {

    private final CheckInRepository checkInRepository;
    private final ImageRepository imageRepository;
    private final ImageAnalysisRepository imageAnalysisRepository;
    private final ReportRepository reportRepository;
    private final CurrentUserIdProvider currentUserIdProvider;
    private final ReportGenerator reportGenerator;
    private final TransactionTemplate readTransactions;
    private final TransactionTemplate writeTransactions;
    private final Clock clock;

    public ReportService(
            CheckInRepository checkInRepository,
            ImageRepository imageRepository,
            ImageAnalysisRepository imageAnalysisRepository,
            ReportRepository reportRepository,
            CurrentUserIdProvider currentUserIdProvider,
            ReportGenerator reportGenerator,
            PlatformTransactionManager transactionManager,
            Clock clock
    ) {
        this.checkInRepository = checkInRepository;
        this.imageRepository = imageRepository;
        this.imageAnalysisRepository = imageAnalysisRepository;
        this.reportRepository = reportRepository;
        this.currentUserIdProvider = currentUserIdProvider;
        this.reportGenerator = reportGenerator;
        this.readTransactions = new TransactionTemplate(transactionManager);
        this.readTransactions.setReadOnly(true);
        this.writeTransactions = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    public ReportResponse create(YearMonth month) {
        requirePastMonth(month);
        long userId = currentUserIdProvider.requireCurrentUserId();
        ReportGenerationRequest generationRequest = requiredTransactionResult(
                readTransactions.execute(status -> prepareGeneration(month, userId)));
        GeneratedContent generatedContent = generate(generationRequest);
        return requiredTransactionResult(writeTransactions.execute(
                status -> save(month, userId, generatedContent)));
    }

    public ReportResponse get(YearMonth month) {
        Objects.requireNonNull(month, "month");
        long userId = currentUserIdProvider.requireCurrentUserId();
        return requiredTransactionResult(readTransactions.execute(status -> reportRepository
                .findByUserIdAndReportMonth(userId, month.atDay(1))
                .map(this::toResponse)
                .orElseThrow(() -> new ApiException(ErrorCode.REPORT_NOT_FOUND))));
    }

    private ReportGenerationRequest prepareGeneration(YearMonth month, long userId) {
        LocalDate reportMonth = month.atDay(1);
        requireReportDoesNotExist(userId, reportMonth);

        List<CheckIn> checkIns = checkInRepository
                .findByUserIdAndCheckInDateBetweenOrderByCheckInDateAscCheckInIdAsc(
                        userId,
                        reportMonth,
                        month.atEndOfMonth());
        if (checkIns.isEmpty()) {
            throw new ApiException(ErrorCode.REPORT_SOURCE_EMPTY);
        }

        Map<Long, Long> imageIdsByCheckInId = imageRepository
                .findReferencesByCheckInIdIn(checkIns.stream().map(CheckIn::id).toList())
                .stream()
                .collect(Collectors.toMap(
                        CheckInImageReference::checkInId,
                        CheckInImageReference::imageId));
        Map<Long, ImageAnalysis> analysesByImageId = imageIdsByCheckInId.isEmpty()
                ? Map.of()
                : imageAnalysisRepository.findByImageIdIn(imageIdsByCheckInId.values())
                        .stream()
                        .collect(Collectors.toMap(ImageAnalysis::imageId, Function.identity()));

        List<ReportTrendPoint> trendPoints = checkIns.stream()
                .map(checkIn -> findTrendPoint(checkIn, imageIdsByCheckInId, analysesByImageId))
                .flatMap(Optional::stream)
                .toList();
        if (trendPoints.size() < 2) {
            throw new ApiException(ErrorCode.REPORT_SOURCE_ERROR);
        }
        return new ReportGenerationRequest(trendPoints);
    }

    private Optional<ReportTrendPoint> findTrendPoint(
            CheckIn checkIn,
            Map<Long, Long> imageIdsByCheckInId,
            Map<Long, ImageAnalysis> analysesByImageId
    ) {
        return Optional.ofNullable(imageIdsByCheckInId.get(checkIn.id()))
                .map(analysesByImageId::get)
                .map(analysis -> new ReportTrendPoint(checkIn.date(), analysis.score()));
    }

    private GeneratedContent generate(ReportGenerationRequest request) {
        try {
            return reportGenerator.generate(request);
        } catch (GenerationException exception) {
            throw new ApiException(errorCodeFor(exception.getReason()));
        }
    }

    private ReportResponse save(YearMonth month, long userId, GeneratedContent generatedContent) {
        LocalDate reportMonth = month.atDay(1);
        requireReportDoesNotExist(userId, reportMonth);
        return toResponse(reportRepository.saveAndFlush(new Report(
                generatedContent.content(),
                month,
                userId)));
    }

    private void requireReportDoesNotExist(long userId, LocalDate reportMonth) {
        if (reportRepository.existsByUserIdAndReportMonth(userId, reportMonth)) {
            throw new ApiException(ErrorCode.REPORT_ALREADY_EXISTS);
        }
    }

    private void requirePastMonth(YearMonth month) {
        Objects.requireNonNull(month, "month");
        if (!month.isBefore(YearMonth.now(clock))) {
            throw new ApiException(ErrorCode.INVALID_REPORT_PERIOD);
        }
    }

    private ErrorCode errorCodeFor(GenerationException.Reason reason) {
        return switch (reason) {
            case UNAVAILABLE -> ErrorCode.AI_PROVIDER_UNAVAILABLE;
            case TIMEOUT -> ErrorCode.AI_GENERATION_TIMEOUT;
            case UPSTREAM, INVALID_OUTPUT -> ErrorCode.REPORT_GENERATION_FAILED;
        };
    }

    private ReportResponse toResponse(Report report) {
        return new ReportResponse(
                report.getReportId(),
                report.getReportMonth(),
                report.getContent());
    }

    private <T> T requiredTransactionResult(T result) {
        return Objects.requireNonNull(result, "transaction result");
    }
}
