package Marshmello.MarshmelloWas.domain.checkin.service;

import Marshmello.MarshmelloWas.domain.auth.port.CurrentUserIdProvider;
import Marshmello.MarshmelloWas.domain.checkin.dto.BodyDiaryResponse;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInEmotionResponse;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInResponse;
import Marshmello.MarshmelloWas.domain.checkin.dto.ImageUrlResponse;
import Marshmello.MarshmelloWas.domain.checkin.dto.MonthlyCheckInCountResponse;
import Marshmello.MarshmelloWas.domain.checkin.dto.MostFrequentBodyRegionResponse;
import Marshmello.MarshmelloWas.domain.checkin.entity.CheckIn;
import Marshmello.MarshmelloWas.domain.checkin.entity.Image;
import Marshmello.MarshmelloWas.domain.checkin.port.ImageStorage;
import Marshmello.MarshmelloWas.domain.checkin.port.ImageStorage.ImageReadUrl;
import Marshmello.MarshmelloWas.domain.checkin.repository.CheckInRepository;
import Marshmello.MarshmelloWas.domain.checkin.repository.BodyDiaryRepository;
import Marshmello.MarshmelloWas.domain.checkin.repository.ImageRepository;
import Marshmello.MarshmelloWas.global.exception.ApiException;
import Marshmello.MarshmelloWas.global.exception.ErrorCode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckInQueryService {

    private final CurrentUserIdProvider currentUserIdProvider;
    private final CheckInRepository checkInRepository;
    private final BodyDiaryRepository bodyDiaryRepository;
    private final ImageRepository imageRepository;
    private final ImageStorage imageStorage;

    public CheckInQueryService(
            CurrentUserIdProvider currentUserIdProvider,
            CheckInRepository checkInRepository,
            BodyDiaryRepository bodyDiaryRepository,
            ImageRepository imageRepository,
            ImageStorage imageStorage
    ) {
        this.currentUserIdProvider = currentUserIdProvider;
        this.checkInRepository = checkInRepository;
        this.bodyDiaryRepository = bodyDiaryRepository;
        this.imageRepository = imageRepository;
        this.imageStorage = imageStorage;
    }

    @Transactional(readOnly = true)
    public List<CheckInResponse> getByDate(LocalDate date) {
        long userId = currentUserIdProvider.requireCurrentUserId();
        List<CheckIn> checkIns = checkInRepository
                .findByUserIdAndCheckInDateBetweenOrderByCheckInDateAscCheckInIdAsc(userId, date, date);
        if (checkIns.isEmpty()) {
            return List.of();
        }

        CheckIn checkIn = checkIns.get(0);
        List<BodyDiaryResponse> bodyDiaries = bodyDiaryRepository
                .findByBodyDiaryIdCheckInIdInOrderByBodyDiaryIdBodyRegionAsc(List.of(checkIn.id()))
                .stream()
                .map(bodyDiary -> new BodyDiaryResponse(
                        bodyDiary.bodyRegion(),
                        bodyDiary.stretchMark(),
                        bodyDiary.comment()))
                .toList();
        return List.of(new CheckInResponse(
                checkIn.id(),
                checkIn.imageId(),
                checkIn.achieved(),
                checkIn.date(),
                checkIn.diary(),
                checkIn.emotion(),
                bodyDiaries));
    }

    @Transactional(readOnly = true)
    public List<CheckInEmotionResponse> getEmotionsByMonth(YearMonth month) {
        long userId = currentUserIdProvider.requireCurrentUserId();
        LocalDate periodStart = month.atDay(1);
        LocalDate periodEnd = month.plusMonths(1).atDay(1);
        return checkInRepository.findEmotionsByUserIdAndPeriod(userId, periodStart, periodEnd);
    }

    @Transactional(readOnly = true)
    public MonthlyCheckInCountResponse getMonthlyCount(YearMonth month) {
        long userId = currentUserIdProvider.requireCurrentUserId();
        LocalDate periodStart = month.atDay(1);
        LocalDate periodEnd = month.plusMonths(1).atDay(1);
        return checkInRepository.findMonthlyCountByUserIdAndPeriod(
                userId,
                periodStart,
                periodEnd);
    }

    @Transactional(readOnly = true)
    public MostFrequentBodyRegionResponse getMostFrequentBodyRegion(YearMonth month) {
        long userId = currentUserIdProvider.requireCurrentUserId();
        LocalDate periodStart = month.atDay(1);
        LocalDate periodEnd = month.plusMonths(1).atDay(1);
        Short bodyRegion = bodyDiaryRepository.findBodyRegionsOrderedByCount(userId, periodStart, periodEnd)
                .stream()
                .findFirst()
                .orElse(null);
        return new MostFrequentBodyRegionResponse(bodyRegion);
    }

    public ImageUrlResponse createImageUrl(long imageId) {
        long userId = currentUserIdProvider.requireCurrentUserId();
        Image image = imageRepository.findByImageIdAndUserIdAndCheckInIsNotNull(imageId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.IMAGE_NOT_FOUND));
        if (image.objectKey() == null) {
            throw new ApiException(ErrorCode.IMAGE_STORAGE_UNAVAILABLE);
        }
        ImageReadUrl readUrl = imageStorage.createReadUrl(image.objectKey());
        return new ImageUrlResponse(image.id(), readUrl.url(), readUrl.expiresAt());
    }
}
