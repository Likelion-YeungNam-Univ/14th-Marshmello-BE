package Marshmello.MarshmelloWas.domain.checkin.service;

import Marshmello.MarshmelloWas.domain.auth.port.CurrentUserIdProvider;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInSummaryResponse;
import Marshmello.MarshmelloWas.domain.checkin.dto.ImageUrlResponse;
import Marshmello.MarshmelloWas.domain.checkin.entity.Image;
import Marshmello.MarshmelloWas.domain.checkin.port.ImageStorage;
import Marshmello.MarshmelloWas.domain.checkin.port.ImageStorage.ImageReadUrl;
import Marshmello.MarshmelloWas.domain.checkin.repository.CheckInRepository;
import Marshmello.MarshmelloWas.domain.checkin.repository.ImageRepository;
import Marshmello.MarshmelloWas.global.exception.ApiException;
import Marshmello.MarshmelloWas.global.exception.ErrorCode;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckInQueryService {

    private final CurrentUserIdProvider currentUserIdProvider;
    private final CheckInRepository checkInRepository;
    private final ImageRepository imageRepository;
    private final ImageStorage imageStorage;

    public CheckInQueryService(
            CurrentUserIdProvider currentUserIdProvider,
            CheckInRepository checkInRepository,
            ImageRepository imageRepository,
            ImageStorage imageStorage
    ) {
        this.currentUserIdProvider = currentUserIdProvider;
        this.checkInRepository = checkInRepository;
        this.imageRepository = imageRepository;
        this.imageStorage = imageStorage;
    }

    @Transactional(readOnly = true)
    public List<CheckInSummaryResponse> getByDate(LocalDate date) {
        long userId = currentUserIdProvider.requireCurrentUserId();
        return checkInRepository.findSummariesByUserIdAndDate(userId, date);
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
