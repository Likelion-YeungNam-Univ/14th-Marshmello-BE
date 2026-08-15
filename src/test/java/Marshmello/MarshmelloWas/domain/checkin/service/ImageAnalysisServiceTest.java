package Marshmello.MarshmelloWas.domain.checkin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import Marshmello.MarshmelloWas.domain.checkin.dto.ImageAnalysisResponse;
import Marshmello.MarshmelloWas.domain.checkin.port.ImageAnalyzer;
import Marshmello.MarshmelloWas.domain.checkin.port.ImageAnalyzer.AnalysisResult;
import Marshmello.MarshmelloWas.domain.checkin.port.ImageAnalyzer.ImageAnalysisException;
import Marshmello.MarshmelloWas.domain.checkin.port.ImageStorage;
import Marshmello.MarshmelloWas.domain.checkin.port.ImageStorage.StoredImage;
import Marshmello.MarshmelloWas.domain.checkin.repository.ImageAnalysisRepository;
import Marshmello.MarshmelloWas.domain.checkin.repository.ImageRepository;
import Marshmello.MarshmelloWas.domain.user.entity.User;
import Marshmello.MarshmelloWas.domain.user.repository.UserRepository;
import Marshmello.MarshmelloWas.global.exception.ApiException;
import Marshmello.MarshmelloWas.global.exception.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;

@SpringBootTest
class ImageAnalysisServiceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ImageAnalysisRepository imageAnalysisRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final ImageAnalyzer imageAnalyzer = mock(ImageAnalyzer.class);
    private final ImageStorage imageStorage = mock(ImageStorage.class);
    private ImageAnalysisService service;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(new User("image-owner", null));
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-15T03:00:00Z"),
                ZoneId.of("Asia/Seoul"));
        service = new ImageAnalysisService(
                user::getUserId,
                imageAnalyzer,
                imageStorage,
                imageRepository,
                imageAnalysisRepository,
                transactionManager,
                clock);
    }

    @AfterEach
    void cleanUp() {
        imageAnalysisRepository.deleteAll();
        imageRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void returnsFalseWithoutStoringWhenDaveyScoreIsNotAvailable() {
        byte[] content = {1, 2, 3};
        when(imageAnalyzer.analyze(content)).thenReturn(AnalysisResult.notDetected());

        ImageAnalysisResponse response = service.analyze(content, "image/png");

        assertThat(response).isEqualTo(new ImageAnalysisResponse(false, null));
        assertThat(imageRepository.count()).isZero();
        assertThat(imageAnalysisRepository.count()).isZero();
        verifyNoInteractions(imageStorage);
    }

    @Test
    void storesDetectedImageAndItsScore() {
        byte[] content = {1, 2, 3};
        when(imageAnalyzer.analyze(content)).thenReturn(AnalysisResult.detected((short) 6));
        when(imageStorage.store(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.same(content),
                org.mockito.ArgumentMatchers.eq("image/png")))
                .thenReturn(new StoredImage("users/1/check-in-images/image", "image/png"));

        ImageAnalysisResponse response = service.analyze(content, "image/png");

        assertThat(response.detected()).isTrue();
        assertThat(imageRepository.findById(response.imageId()).orElseThrow().objectKey())
                .isEqualTo("users/1/check-in-images/image");
        assertThat(imageAnalysisRepository.findById(response.imageId()).orElseThrow().score())
                .isEqualTo((short) 6);
    }

    @Test
    void rejectsImagesLargerThanTenMebibytesBeforeAnalysis() {
        byte[] oversized = new byte[ImageAnalysisService.MAX_IMAGE_SIZE + 1];

        assertThatThrownBy(() -> service.analyze(oversized, null))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.IMAGE_TOO_LARGE));
        verifyNoInteractions(imageAnalyzer, imageStorage);
    }

    @Test
    void mapsUnavailableAnalyzerToRetryableApiError() {
        byte[] content = {1};
        when(imageAnalyzer.analyze(content)).thenThrow(new ImageAnalysisException(
                ImageAnalysisException.Reason.UNAVAILABLE));

        assertThatThrownBy(() -> service.analyze(content, null))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.IMAGE_ANALYZER_UNAVAILABLE));
        verify(imageStorage, never()).store(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }
}
