package Marshmello.MarshmelloWas.domain.checkin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import Marshmello.MarshmelloWas.domain.auth.port.CurrentUserIdProvider;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInEmotionResponse;
import Marshmello.MarshmelloWas.domain.checkin.dto.MonthlyCheckInCountResponse;
import Marshmello.MarshmelloWas.domain.checkin.dto.MostFrequentBodyRegionResponse;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInSummaryResponse;
import Marshmello.MarshmelloWas.domain.checkin.entity.BodyDiary;
import Marshmello.MarshmelloWas.domain.checkin.entity.BodyRegion;
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
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class CheckInQueryServiceTest {

    @Autowired
    private CheckInQueryService service;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

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
    void returnsTheExactCountOfCurrentUsersCheckInsWithinTheRequestedMonth() {
        User owner = userRepository.save(new User("count-owner", null));
        when(currentUserIdProvider.requireCurrentUserId()).thenReturn(owner.getUserId());
        saveCheckIn(owner.getUserId(), LocalDate.of(2026, 8, 1), "test/count-first-day", true);
        saveCheckIn(owner.getUserId(), LocalDate.of(2026, 8, 17), "test/count-middle-day", false);
        saveCheckIn(owner.getUserId(), LocalDate.of(2026, 8, 31), "test/count-last-day", true);

        assertThat(service.getMonthlyCount(YearMonth.of(2026, 8)))
                .isEqualTo(new MonthlyCheckInCountResponse(3, 2));
    }

    @Test
    void retrievesMonthlyCountsWithOnePreparedStatement() {
        User owner = userRepository.save(new User("query-owner", null));
        when(currentUserIdProvider.requireCurrentUserId()).thenReturn(owner.getUserId());
        saveCheckIn(owner.getUserId(), LocalDate.of(2026, 8, 1), "test/count-query-first", true);
        saveCheckIn(owner.getUserId(), LocalDate.of(2026, 8, 17), "test/count-query-middle", false);
        saveCheckIn(owner.getUserId(), LocalDate.of(2026, 8, 31), "test/count-query-last", true);
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        boolean statisticsEnabled = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        try {
            MonthlyCheckInCountResponse response = service.getMonthlyCount(YearMonth.of(2026, 8));

            assertThat(response).isEqualTo(new MonthlyCheckInCountResponse(3, 2));
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        } finally {
            statistics.clear();
            statistics.setStatisticsEnabled(statisticsEnabled);
        }
    }

    @Test
    void excludesAnotherUsersCheckInsFromTheMonthlyCount() {
        User owner = userRepository.save(new User("count-owner", null));
        User other = userRepository.save(new User("count-other", null));
        when(currentUserIdProvider.requireCurrentUserId()).thenReturn(owner.getUserId());
        saveCheckIn(owner.getUserId(), LocalDate.of(2026, 8, 15), "test/count-owner", false);
        saveCheckIn(other.getUserId(), LocalDate.of(2026, 8, 16), "test/count-other-user", true);

        assertThat(service.getMonthlyCount(YearMonth.of(2026, 8)))
                .isEqualTo(new MonthlyCheckInCountResponse(1, 0));
    }

    @Test
    void excludesPreviousMonthsCheckInsFromTheMonthlyCount() {
        User owner = userRepository.save(new User("count-owner", null));
        when(currentUserIdProvider.requireCurrentUserId()).thenReturn(owner.getUserId());
        saveCheckIn(owner.getUserId(), LocalDate.of(2026, 7, 31), "test/count-previous-month", true);
        saveCheckIn(owner.getUserId(), LocalDate.of(2026, 8, 1), "test/count-current-month", false);

        assertThat(service.getMonthlyCount(YearMonth.of(2026, 8)))
                .isEqualTo(new MonthlyCheckInCountResponse(1, 0));
    }

    @Test
    void excludesNextMonthsCheckInsFromTheMonthlyCount() {
        User owner = userRepository.save(new User("count-owner", null));
        when(currentUserIdProvider.requireCurrentUserId()).thenReturn(owner.getUserId());
        saveCheckIn(owner.getUserId(), LocalDate.of(2026, 8, 31), "test/count-current-month", false);
        saveCheckIn(owner.getUserId(), LocalDate.of(2026, 9, 1), "test/count-next-month", true);

        assertThat(service.getMonthlyCount(YearMonth.of(2026, 8)))
                .isEqualTo(new MonthlyCheckInCountResponse(1, 0));
    }

    @Test
    void includesTheLastDayOfTheMonthInTheMonthlyCount() {
        User owner = userRepository.save(new User("count-owner", null));
        when(currentUserIdProvider.requireCurrentUserId()).thenReturn(owner.getUserId());
        saveCheckIn(owner.getUserId(), LocalDate.of(2026, 8, 31), "test/count-last-day", true);

        assertThat(service.getMonthlyCount(YearMonth.of(2026, 8)))
                .isEqualTo(new MonthlyCheckInCountResponse(1, 1));
    }

    @Test
    void returnsZeroWhenTheRequestedMonthHasNoCheckIns() {
        User owner = userRepository.save(new User("empty-count", null));
        when(currentUserIdProvider.requireCurrentUserId()).thenReturn(owner.getUserId());

        assertThat(service.getMonthlyCount(YearMonth.of(2026, 8)))
                .isEqualTo(new MonthlyCheckInCountResponse(0, 0));
    }

    @Test
    void returnsMostFrequentBodyRegionForTheCurrentUserInTheRequestedMonth() {
        User owner = userRepository.save(new User("body-owner", null));
        when(currentUserIdProvider.requireCurrentUserId()).thenReturn(owner.getUserId());
        saveBodyDiary(owner.getUserId(), LocalDate.of(2026, 8, 2), BodyRegion.CHEST);
        saveBodyDiary(owner.getUserId(), LocalDate.of(2026, 8, 7), BodyRegion.ABDOMEN);
        saveBodyDiary(owner.getUserId(), LocalDate.of(2026, 8, 15), BodyRegion.ABDOMEN);

        assertThat(service.getMostFrequentBodyRegion(YearMonth.of(2026, 8)))
                .isEqualTo(new MostFrequentBodyRegionResponse((short) 2));
    }

    @Test
    void excludesAnotherUsersBodyDiariesFromTheMonthlyRegionCount() {
        User owner = userRepository.save(new User("body-owner", null));
        User other = userRepository.save(new User("body-other", null));
        when(currentUserIdProvider.requireCurrentUserId()).thenReturn(owner.getUserId());
        saveBodyDiary(owner.getUserId(), LocalDate.of(2026, 8, 2), BodyRegion.ABDOMEN);
        saveBodyDiary(owner.getUserId(), LocalDate.of(2026, 8, 7), BodyRegion.ABDOMEN);
        saveBodyDiary(other.getUserId(), LocalDate.of(2026, 8, 3), BodyRegion.RIGHT_LEG);
        saveBodyDiary(other.getUserId(), LocalDate.of(2026, 8, 4), BodyRegion.RIGHT_LEG);
        saveBodyDiary(other.getUserId(), LocalDate.of(2026, 8, 5), BodyRegion.RIGHT_LEG);

        assertThat(service.getMostFrequentBodyRegion(YearMonth.of(2026, 8)))
                .isEqualTo(new MostFrequentBodyRegionResponse((short) 2));
    }

    @Test
    void excludesBodyDiariesOutsideTheRequestedMonth() {
        User owner = userRepository.save(new User("body-owner", null));
        when(currentUserIdProvider.requireCurrentUserId()).thenReturn(owner.getUserId());
        saveBodyDiary(owner.getUserId(), LocalDate.of(2026, 8, 2), BodyRegion.RIGHT_LEG);
        saveBodyDiary(owner.getUserId(), LocalDate.of(2026, 7, 31), BodyRegion.ABDOMEN);
        saveBodyDiary(owner.getUserId(), LocalDate.of(2026, 9, 1), BodyRegion.ABDOMEN);

        assertThat(service.getMostFrequentBodyRegion(YearMonth.of(2026, 8)))
                .isEqualTo(new MostFrequentBodyRegionResponse((short) 8));
    }

    @Test
    void returnsTheSmallestRegionCodeWhenMonthlyBodyDiaryCountsAreTied() {
        User owner = userRepository.save(new User("body-owner", null));
        when(currentUserIdProvider.requireCurrentUserId()).thenReturn(owner.getUserId());
        saveBodyDiary(owner.getUserId(), LocalDate.of(2026, 8, 2), BodyRegion.PELVIS);
        saveBodyDiary(owner.getUserId(), LocalDate.of(2026, 8, 7), BodyRegion.ABDOMEN);

        assertThat(service.getMostFrequentBodyRegion(YearMonth.of(2026, 8)))
                .isEqualTo(new MostFrequentBodyRegionResponse((short) 2));
    }

    @Test
    void returnsNullWhenTheRequestedMonthHasNoBodyDiaries() {
        User owner = userRepository.save(new User("empty-region", null));
        when(currentUserIdProvider.requireCurrentUserId()).thenReturn(owner.getUserId());

        assertThat(service.getMostFrequentBodyRegion(YearMonth.of(2026, 8)))
                .isEqualTo(new MostFrequentBodyRegionResponse(null));
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
        return saveCheckIn(userId, date, objectKey, (short) 1, false);
    }

    private Image saveCheckIn(long userId, LocalDate date, String objectKey, short emotion) {
        return saveCheckIn(userId, date, objectKey, emotion, false);
    }

    private Image saveCheckIn(long userId, LocalDate date, String objectKey, boolean achieved) {
        return saveCheckIn(userId, date, objectKey, (short) 1, achieved);
    }

    private Image saveCheckIn(long userId, LocalDate date, String objectKey, short emotion, boolean achieved) {
        CheckIn checkIn = checkInRepository.save(new CheckIn(achieved, date, null, emotion, userId));
        Image image = new Image(userId, objectKey, "image/png", Instant.now());
        image.attachTo(checkIn, userId);
        imageRepository.save(image);
        imageAnalysisRepository.save(new ImageAnalysis(image.id(), (short) 4));
        return image;
    }

    private void saveBodyDiary(long userId, LocalDate date, BodyRegion bodyRegion) {
        CheckIn checkIn = checkInRepository.save(new CheckIn(false, date, null, (short) 1, userId));
        bodyDiaryRepository.save(new BodyDiary(bodyRegion, checkIn, null, null));
    }

    private void assertImageNotFound(long imageId) {
        assertThatThrownBy(() -> service.createImageUrl(imageId))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.IMAGE_NOT_FOUND));
    }
}
