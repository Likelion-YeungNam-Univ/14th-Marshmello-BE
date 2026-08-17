package Marshmello.MarshmelloWas.domain.checkin.service;

import Marshmello.MarshmelloWas.domain.auth.port.CurrentUserIdProvider;
import Marshmello.MarshmelloWas.domain.checkin.dto.BodyDiaryRequest;
import Marshmello.MarshmelloWas.domain.checkin.dto.BodyDiaryResponse;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInCreateRequest;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInResponse;
import Marshmello.MarshmelloWas.domain.checkin.entity.BodyDiary;
import Marshmello.MarshmelloWas.domain.checkin.entity.BodyRegion;
import Marshmello.MarshmelloWas.domain.checkin.entity.CheckIn;
import Marshmello.MarshmelloWas.domain.checkin.entity.Image;
import Marshmello.MarshmelloWas.domain.checkin.repository.BodyDiaryRepository;
import Marshmello.MarshmelloWas.domain.checkin.repository.CheckInRepository;
import Marshmello.MarshmelloWas.domain.checkin.repository.ImageAnalysisRepository;
import Marshmello.MarshmelloWas.domain.checkin.repository.ImageRepository;
import Marshmello.MarshmelloWas.global.exception.ApiException;
import Marshmello.MarshmelloWas.global.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckInService {

    private final CurrentUserIdProvider currentUserIdProvider;
    private final CheckInRepository checkInRepository;
    private final ImageRepository imageRepository;
    private final ImageAnalysisRepository imageAnalysisRepository;
    private final BodyDiaryRepository bodyDiaryRepository;
    private final Clock clock;

    public CheckInService(
            CurrentUserIdProvider currentUserIdProvider,
            CheckInRepository checkInRepository,
            ImageRepository imageRepository,
            ImageAnalysisRepository imageAnalysisRepository,
            BodyDiaryRepository bodyDiaryRepository,
            Clock clock
    ) {
        this.currentUserIdProvider = currentUserIdProvider;
        this.checkInRepository = checkInRepository;
        this.imageRepository = imageRepository;
        this.imageAnalysisRepository = imageAnalysisRepository;
        this.bodyDiaryRepository = bodyDiaryRepository;
        this.clock = clock;
    }

    @Transactional
    public CheckInResponse create(CheckInCreateRequest request) {
        return create(request, LocalDate.now(clock));
    }

    @Transactional
    public CheckInResponse create(CheckInCreateRequest request, LocalDate date) {
        validateDistinctBodyRegions(request.bodyDiaries());
        long userId = currentUserIdProvider.requireCurrentUserId();
        if (checkInRepository.existsByUserIdAndCheckInDate(userId, date)) {
            throw new ApiException(ErrorCode.CHECK_IN_ALREADY_EXISTS);
        }

        Image image = imageRepository.findByIdForUpdate(request.imageId())
                .filter(candidate -> candidate.belongsTo(userId))
                .orElseThrow(() -> new ApiException(ErrorCode.IMAGE_NOT_FOUND));
        if (image.isAttached()) {
            throw new ApiException(ErrorCode.IMAGE_ALREADY_USED);
        }
        if (!imageAnalysisRepository.existsById(image.id())) {
            throw new ApiException(ErrorCode.IMAGE_ANALYSIS_REQUIRED);
        }

        CheckIn checkIn = saveCheckIn(request, userId, date);
        image.attachTo(checkIn, userId);
        List<BodyDiary> bodyDiaries = bodyDiaryRepository.saveAll(request.bodyDiaries().stream()
                .map(bodyDiary -> toEntity(bodyDiary, checkIn))
                .toList());
        return toResponse(checkIn, image, bodyDiaries);
    }

    private void validateDistinctBodyRegions(List<BodyDiaryRequest> bodyDiaries) {
        Set<Short> bodyRegions = new HashSet<>();
        boolean duplicated = bodyDiaries.stream()
                .map(BodyDiaryRequest::bodyRegion)
                .anyMatch(bodyRegion -> !bodyRegions.add(bodyRegion));
        if (duplicated) {
            throw new ApiException(ErrorCode.DUPLICATE_BODY_REGION);
        }
    }

    private CheckIn saveCheckIn(CheckInCreateRequest request, long userId, LocalDate date) {
        try {
            return checkInRepository.saveAndFlush(new CheckIn(
                    request.achieved(),
                    date,
                    request.diary(),
                    request.emotion(),
                    userId));
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(ErrorCode.CHECK_IN_ALREADY_EXISTS);
        }
    }

    private BodyDiary toEntity(BodyDiaryRequest request, CheckIn checkIn) {
        return new BodyDiary(
                BodyRegion.fromCode(request.bodyRegion()),
                checkIn,
                request.stretchMark(),
                request.comment());
    }

    private CheckInResponse toResponse(CheckIn checkIn, Image image, List<BodyDiary> bodyDiaries) {
        return new CheckInResponse(
                checkIn.id(),
                image.id(),
                checkIn.achieved(),
                checkIn.date(),
                checkIn.diary(),
                checkIn.emotion(),
                bodyDiaries.stream()
                        .map(bodyDiary -> new BodyDiaryResponse(
                                bodyDiary.bodyRegion(),
                                bodyDiary.stretchMark(),
                                bodyDiary.comment()))
                        .toList());
    }
}
