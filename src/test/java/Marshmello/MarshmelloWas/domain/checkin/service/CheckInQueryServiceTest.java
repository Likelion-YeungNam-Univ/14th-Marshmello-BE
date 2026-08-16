package Marshmello.MarshmelloWas.domain.checkin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import Marshmello.MarshmelloWas.domain.auth.port.CurrentUserIdProvider;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInEmotionResponse;
import Marshmello.MarshmelloWas.domain.checkin.dto.MonthlyCheckInCountResponse;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInSummaryResponse;
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
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
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
    void returnsOnlyCurrentUsersCheckInForTheRequestedDate() {
        User owner = userRepository.save(new User("timeline-owner", null));
        User other = userRepository.save(new User("timeline-other", null));
        when(currentUserIdProvider.requireCurrentUserId()).thenReturn(owner.getUserId());
        saveCheckIn(owner.getUserId(), LocalDate.of(2026, 8, 13), "test/old");
        Image newest = saveCheckIn(owner.getUserId(), LocalDate.of(2026, 8, 15), "test/new");
        saveCheckIn(other.getUserId(), LocalDate.of(2026, 8, 14), "test/other");

        List<CheckInSummaryResponse> result = service.getByDate(LocalDate.of(2026, 8, 15));

        assertThat(result).singleElement()
                .extracting(CheckInSummaryResponse::imageId)
                .isEqualTo(newest.id());
    }

    @Test
    void returnsEmptyListWhenTheDateHasNoCheckIn() {
        User owner = userRepository.save(new User("date-owner", null));
        when(currentUserIdProvider.requireCurrentUserId()).thenReturn(owner.getUserId());
        saveCheckIn(owner.getUserId(), LocalDate.of(2026, 8, 15), "test/date");

        assertThat(service.getByDate(LocalDate.of(2026, 8, 14))).isEmpty();
    }

    @Test
    void returnsMonthlyEmotionsInCheckInDateOrder() {
        User owner = userRepository.save(new User("emotion-owner", null));
        User other = userRepository.save(new User("emotion-other", null));
        when(currentUserIdProvider.requireCurrentUserId()).thenReturn(owner.getUserId());
        saveCheckIn(owner.getUserId(), LocalDate.of(2026, 8, 20), "test/emotion-late", (short) 4);
        saveCheckIn(owner.getUserId(), LocalDate.of(2026, 8, 2), "test/emotion-early", (short) 2);
        saveCheckIn(owner.getUserId(), LocalDate.of(2026, 7, 31), "test/emotion-july", (short) 1);
        saveCheckIn(other.getUserId(), LocalDate.of(2026, 8, 10), "test/emotion-other", (short) 3);

        assertThat(service.getEmotionsByMonth(YearMonth.of(2026, 8)))
                .containsExactly(
                        new CheckInEmotionResponse(LocalDate.of(2026, 8, 2), (short) 2),
                        new CheckInEmotionResponse(LocalDate.of(2026, 8, 20), (short) 4));
        assertThat(service.getEmotionsByMonth(YearMonth.of(2026, 9))).isEmpty();
    }

    @Test
    void returnsOnlyCurrentUsersCheckInsWithinTheRequestedMonth() {
        User owner = userRepository.save(new User("count-owner", null));
        User other = userRepository.save(new User("count-other", null));
        when(currentUserIdProvider.requireCurrentUserId()).thenReturn(owner.getUserId());
        saveCheckIn(owner.getUserId(), LocalDate.of(2026, 7, 31), "test/count-previous-month");
        saveCheckIn(owner.getUserId(), LocalDate.of(2026, 8, 1), "test/count-first-day");
        saveCheckIn(owner.getUserId(), LocalDate.of(2026, 8, 17), "test/count-middle-day");
        saveCheckIn(owner.getUserId(), LocalDate.of(2026, 8, 31), "test/count-last-day");
        saveCheckIn(owner.getUserId(), LocalDate.of(2026, 9, 1), "test/count-next-month");
        saveCheckIn(other.getUserId(), LocalDate.of(2026, 8, 15), "test/count-other-user");

        assertThat(service.getMonthlyCount(YearMonth.of(2026, 8)))
                .isEqualTo(new MonthlyCheckInCountResponse(3));
    }

    @Test
    void returnsZeroWhenTheRequestedMonthHasNoCheckIns() {
        User owner = userRepository.save(new User("empty-count", null));
        when(currentUserIdProvider.requireCurrentUserId()).thenReturn(owner.getUserId());

        assertThat(service.getMonthlyCount(YearMonth.of(2026, 8)))
                .isEqualTo(new MonthlyCheckInCountResponse(0));
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
        return saveCheckIn(userId, date, objectKey, (short) 1);
    }

    private Image saveCheckIn(long userId, LocalDate date, String objectKey, short emotion) {
        CheckIn checkIn = checkInRepository.save(new CheckIn(false, date, null, emotion, userId));
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
