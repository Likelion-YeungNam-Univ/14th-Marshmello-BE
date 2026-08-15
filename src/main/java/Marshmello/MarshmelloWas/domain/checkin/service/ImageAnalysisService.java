package Marshmello.MarshmelloWas.domain.checkin.service;

import Marshmello.MarshmelloWas.domain.auth.port.CurrentUserIdProvider;
import Marshmello.MarshmelloWas.domain.checkin.dto.ImageAnalysisResponse;
import Marshmello.MarshmelloWas.domain.checkin.entity.Image;
import Marshmello.MarshmelloWas.domain.checkin.entity.ImageAnalysis;
import Marshmello.MarshmelloWas.domain.checkin.port.ImageAnalyzer;
import Marshmello.MarshmelloWas.domain.checkin.port.ImageAnalyzer.AnalysisResult;
import Marshmello.MarshmelloWas.domain.checkin.port.ImageAnalyzer.ImageAnalysisException;
import Marshmello.MarshmelloWas.domain.checkin.port.ImageStorage;
import Marshmello.MarshmelloWas.domain.checkin.port.ImageStorage.StoredImage;
import Marshmello.MarshmelloWas.domain.checkin.repository.ImageAnalysisRepository;
import Marshmello.MarshmelloWas.domain.checkin.repository.ImageRepository;
import Marshmello.MarshmelloWas.global.exception.ApiException;
import Marshmello.MarshmelloWas.global.exception.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ImageAnalysisService {

    static final int MAX_IMAGE_SIZE = 10 * 1024 * 1024;

    private final CurrentUserIdProvider currentUserIdProvider;
    private final ImageAnalyzer imageAnalyzer;
    private final ImageStorage imageStorage;
    private final ImageRepository imageRepository;
    private final ImageAnalysisRepository imageAnalysisRepository;
    private final TransactionTemplate writeTransaction;
    private final Clock clock;

    public ImageAnalysisService(
            CurrentUserIdProvider currentUserIdProvider,
            ImageAnalyzer imageAnalyzer,
            ImageStorage imageStorage,
            ImageRepository imageRepository,
            ImageAnalysisRepository imageAnalysisRepository,
            PlatformTransactionManager transactionManager,
            Clock clock
    ) {
        this.currentUserIdProvider = currentUserIdProvider;
        this.imageAnalyzer = imageAnalyzer;
        this.imageStorage = imageStorage;
        this.imageRepository = imageRepository;
        this.imageAnalysisRepository = imageAnalysisRepository;
        this.writeTransaction = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    public ImageAnalysisResponse analyze(byte[] imageContent, String contentType) {
        validateImage(imageContent);
        long userId = currentUserIdProvider.requireCurrentUserId();
        AnalysisResult result = analyzeImage(imageContent);
        if (!result.detected()) {
            return new ImageAnalysisResponse(false, null);
        }

        StoredImage storedImage = imageStorage.store(userId, imageContent, contentType);
        try {
            return Objects.requireNonNull(writeTransaction.execute(status ->
                    saveAnalysis(userId, storedImage, result.score())));
        } catch (RuntimeException exception) {
            deleteStoredImage(storedImage.objectKey(), exception);
            throw exception;
        }
    }

    private void validateImage(byte[] imageContent) {
        if (imageContent == null || imageContent.length == 0) {
            throw new ApiException(ErrorCode.IMAGE_ANALYSIS_FAILED);
        }
        if (imageContent.length > MAX_IMAGE_SIZE) {
            throw new ApiException(ErrorCode.IMAGE_TOO_LARGE);
        }
    }

    private AnalysisResult analyzeImage(byte[] imageContent) {
        try {
            return imageAnalyzer.analyze(imageContent);
        } catch (ImageAnalysisException exception) {
            ErrorCode errorCode = exception.reason() == ImageAnalysisException.Reason.UNAVAILABLE
                    ? ErrorCode.IMAGE_ANALYZER_UNAVAILABLE
                    : ErrorCode.IMAGE_ANALYSIS_FAILED;
            throw new ApiException(errorCode);
        }
    }

    private ImageAnalysisResponse saveAnalysis(long userId, StoredImage storedImage, short score) {
        Image image = imageRepository.saveAndFlush(new Image(
                userId,
                storedImage.objectKey(),
                storedImage.contentType(),
                Instant.now(clock)));
        imageAnalysisRepository.save(new ImageAnalysis(image.id(), score));
        return new ImageAnalysisResponse(true, image.id());
    }

    private void deleteStoredImage(String objectKey, RuntimeException originalFailure) {
        try {
            imageStorage.delete(objectKey);
        } catch (RuntimeException cleanupFailure) {
            originalFailure.addSuppressed(cleanupFailure);
        }
    }
}
