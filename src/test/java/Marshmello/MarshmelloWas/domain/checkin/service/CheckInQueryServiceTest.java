package Marshmello.MarshmelloWas.domain.checkin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import Marshmello.MarshmelloWas.domain.auth.port.CurrentUserIdProvider;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInTimelineItemResponse;
import Marshmello.MarshmelloWas.domain.checkin.dto.ImageUrlResponse;
import Marshmello.MarshmelloWas.domain.checkin.entity.CheckIn;
import Marshmello.MarshmelloWas.domain.checkin.entity.Image;
import Marshmello.MarshmelloWas.domain.checkin.entity.ImageAnalysis;
import Marshmello.MarshmelloWas.domain.checkin.port.ImageStorage;
import Marshmello.MarshmelloWas.domain.checkin.port.ImageStorage.ImageReadUrl;
import Marshmello.MarshmelloWas.domain.checkin.repository.BodyDiaryRepository;
import Marshmello.MarshmelloWas.domain.checkin.repository.CheckInRepository;
import Marshmello.MarshmelloWas.domain.checkin.repository.ImageAnalysisRepository;
import Marshmello.MarshmelloWas.domain.checkin.repository.ImageRepository;
import Marshmello.MarshmelloWas.domain.user.entity.User;
import Marshmello.MarshmelloWas.domain.user.repository.UserRepository;
import Marshmello.MarshmelloWas.global.exception.ApiException;
import Marshmello.MarshmelloWas.global.exception.ErrorCode;
import Marshmello.MarshmelloWas.global.web.PageResponse;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class CheckInQueryServiceTest {

    @Autowired
    private CheckInQueryService service;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CheckInRepository checkInRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ImageAnalysisRepository imageAnalysisRepository;

    @Autowired
    private BodyDiaryRepository bodyDiaryRepository;

    @MockitoBean(name = "oidcCurrentUserIdProvider")
    private CurrentUserIdProvider currentUserIdProvider;

    @MockitoBean
    private ImageStorage imageStorage;

    @AfterEach
    void cleanUp() {
        bodyDiaryRepository.deleteAll();
        imageAnalysisRepository.deleteAll();
        imageRepository.deleteAll();
        checkInRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void returnsCurrentUsersCheckInsNewestFirstWithImageIds() {
        User owner = userRepository.save(new User("timeline-owner", null));
        User other = userRepository.save(new User("timeline-other", null));
        when(currentUserIdProvider.requireCurrentUserId()).thenReturn(owner.getUserId());
        saveCheckIn(owner.getUserId(), LocalDate.of(2026, 8, 13), "test/old");
        Image newest = saveCheckIn(owner.getUserId(), LocalDate.of(2026, 8, 15), "test/new");
        saveCheckIn(other.getUserId(), LocalDate.of(2026, 8, 14), "test/other");

        PageResponse<CheckInTimelineItemResponse> timeline = service.getTimeline(0, 1);

        assertThat(timeline.totalElements()).isEqualTo(2);
        assertThat(timeline.totalPages()).isEqualTo(2);
        assertThat(timeline.content()).singleElement()
                .extracting(CheckInTimelineItemResponse::imageId)
                .isEqualTo(newest.id());
    }

    @Test
    void createsReadUrlOnlyForOwnedAttachedImage() {
        User owner = userRepository.save(new User("url-owner", null));
        when(currentUserIdProvider.requireCurrentUserId()).thenReturn(owner.getUserId());
        Image image = saveCheckIn(owner.getUserId(), LocalDate.of(2026, 8, 15), "test/url-image");
        ImageReadUrl readUrl = new ImageReadUrl(
                URI.create("https://bucket.example.test/test/url-image?signature=redacted"),
                Instant.parse("2026-08-15T04:00:00Z"));
        when(imageStorage.createReadUrl("test/url-image")).thenReturn(readUrl);

        ImageUrlResponse response = service.createImageUrl(image.id());

        assertThat(response).isEqualTo(new ImageUrlResponse(image.id(), readUrl.url(), readUrl.expiresAt()));
        verify(imageStorage).createReadUrl("test/url-image");
    }

    @Test
    void hidesPendingAndAnotherUsersImages() {
        User owner = userRepository.save(new User("url-owner", null));
        User other = userRepository.save(new User("url-other", null));
        when(currentUserIdProvider.requireCurrentUserId()).thenReturn(owner.getUserId());
        Image pending = imageRepository.save(new Image(
                owner.getUserId(), "test/pending", "image/png", Instant.now()));
        Image anotherUsersImage = saveCheckIn(
                other.getUserId(), LocalDate.of(2026, 8, 15), "test/other-image");

        assertImageNotFound(pending.id());
        assertImageNotFound(anotherUsersImage.id());
    }

    private Image saveCheckIn(long userId, LocalDate date, String objectKey) {
        CheckIn checkIn = checkInRepository.save(new CheckIn(false, date, null, (short) 1, userId));
        Image image = new Image(userId, objectKey, "image/png", Instant.now());
        image.attachTo(checkIn, userId);
        imageRepository.save(image);
        imageAnalysisRepository.save(new ImageAnalysis(image.id(), (short) 4));
        return image;
    }

    private void assertImageNotFound(long imageId) {
        assertThatThrownBy(() -> service.createImageUrl(imageId))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.IMAGE_NOT_FOUND));
    }
}
