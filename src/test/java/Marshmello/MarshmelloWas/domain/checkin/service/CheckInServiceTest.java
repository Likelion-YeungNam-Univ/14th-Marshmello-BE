package Marshmello.MarshmelloWas.domain.checkin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import Marshmello.MarshmelloWas.domain.auth.port.CurrentUserIdProvider;
import Marshmello.MarshmelloWas.domain.checkin.dto.BodyDiaryRequest;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInCreateRequest;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInResponse;
import Marshmello.MarshmelloWas.domain.checkin.entity.Image;
import Marshmello.MarshmelloWas.domain.checkin.entity.ImageAnalysis;
import Marshmello.MarshmelloWas.domain.checkin.repository.BodyDiaryRepository;
import Marshmello.MarshmelloWas.domain.checkin.repository.CheckInRepository;
import Marshmello.MarshmelloWas.domain.checkin.repository.ImageAnalysisRepository;
import Marshmello.MarshmelloWas.domain.checkin.repository.ImageRepository;
import Marshmello.MarshmelloWas.domain.user.entity.User;
import Marshmello.MarshmelloWas.domain.user.repository.UserRepository;
import Marshmello.MarshmelloWas.global.exception.ApiException;
import Marshmello.MarshmelloWas.global.exception.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class CheckInServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 15);

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

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private CheckInService service;

    @MockitoBean
    private Clock clock;

    @MockitoBean(name = "oidcCurrentUserIdProvider")
    private CurrentUserIdProvider currentUserIdProvider;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(Instant.parse("2026-08-15T03:00:00Z"));
        when(clock.getZone()).thenReturn(java.time.ZoneId.of("Asia/Seoul"));
    }

    @AfterEach
    void cleanUp() {
        bodyDiaryRepository.deleteAll();
        imageAnalysisRepository.deleteAll();
        imageRepository.deleteAll();
        checkInRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createsTodaysCheckInFromAnalyzedImageAndAllowsEmptyBodyDiary() {
        User user = saveCurrentUser();
        Image image = saveAnalyzedImage(user.getUserId(), (short) 5);

        CheckInResponse response = service.create(new CheckInCreateRequest(
                image.id(), true, "오늘의 기록", (short) 3, List.of()));

        assertThat(response.checkInDate()).isEqualTo(TODAY);
        assertThat(response.imageId()).isEqualTo(image.id());
        assertThat(response.bodyDiaries()).isEmpty();
        TransactionTemplate readTransaction = new TransactionTemplate(transactionManager);
        Long attachedCheckInId = readTransaction.execute(status ->
                imageRepository.findById(image.id()).orElseThrow().checkInId());
        Long reverseImageId = readTransaction.execute(status ->
                checkInRepository.findById(response.checkInId()).orElseThrow().imageId());
        assertThat(attachedCheckInId).isEqualTo(response.checkInId());
        assertThat(reverseImageId).isEqualTo(image.id());
    }

    @Test
    void createsCheckInForTheRequestedDate() {
        User user = saveCurrentUser();
        Image image = saveAnalyzedImage(user.getUserId(), (short) 5);
        LocalDate requestedDate = LocalDate.of(2026, 8, 10);

        CheckInResponse response = service.create(
                new CheckInCreateRequest(image.id(), false, null, (short) 1, List.of()), requestedDate);

        assertThat(response.checkInDate()).isEqualTo(requestedDate);
        assertThat(checkInRepository.findById(response.checkInId()).orElseThrow().date())
                .isEqualTo(requestedDate);
    }

    @Test
    void storesNullableStretchMarkAndNumericBodyRegions() {
        User user = saveCurrentUser();
        Image image = saveAnalyzedImage(user.getUserId(), (short) 3);

        CheckInResponse response = service.create(new CheckInCreateRequest(
                image.id(),
                false,
                null,
                (short) 1,
                List.of(new BodyDiaryRequest((short) 8, null, "오른쪽 다리"))));

        assertThat(response.bodyDiaries()).containsExactly(
                new Marshmello.MarshmelloWas.domain.checkin.dto.BodyDiaryResponse(
                        (short) 8, null, "오른쪽 다리"));
        assertThat(bodyDiaryRepository.findAll()).singleElement()
                .satisfies(bodyDiary -> {
                    assertThat(bodyDiary.bodyRegion()).isEqualTo((short) 8);
                    assertThat(bodyDiary.stretchMark()).isNull();
                });
    }

    @Test
    void rejectsDuplicateBodyRegionWithDedicatedCode() {
        User user = saveCurrentUser();
        Image image = saveAnalyzedImage(user.getUserId(), (short) 2);
        CheckInCreateRequest request = new CheckInCreateRequest(
                image.id(),
                false,
                null,
                (short) 2,
                List.of(
                        new BodyDiaryRequest((short) 1, true, null),
                        new BodyDiaryRequest((short) 1, false, null)));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.DUPLICATE_BODY_REGION));
        assertThat(checkInRepository.count()).isZero();
    }

    @Test
    void rejectsSecondCheckInOnTheSameServerDate() {
        User user = saveCurrentUser();
        Image firstImage = saveAnalyzedImage(user.getUserId(), (short) 2);
        Image secondImage = saveAnalyzedImage(user.getUserId(), (short) 4);
        service.create(requestFor(firstImage.id()));

        assertThatThrownBy(() -> service.create(requestFor(secondImage.id())))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.CHECK_IN_ALREADY_EXISTS));
        assertThat(checkInRepository.count()).isOne();
    }

    @Test
    void hidesAnotherUsersImage() {
        saveCurrentUser();
        User other = userRepository.save(new User("other", null));
        Image image = saveAnalyzedImage(other.getUserId(), (short) 4);

        assertThatThrownBy(() -> service.create(requestFor(image.id())))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.IMAGE_NOT_FOUND));
    }

    @Test
    void rejectsImageWithoutSuccessfulAnalysis() {
        User user = saveCurrentUser();
        Image image = imageRepository.save(new Image(
                user.getUserId(), "test/not-analyzed", "image/png", Instant.now()));

        assertThatThrownBy(() -> service.create(requestFor(image.id())))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.IMAGE_ANALYSIS_REQUIRED));
    }

    private User saveCurrentUser() {
        User user = userRepository.save(new User("checkin-owner", null));
        when(currentUserIdProvider.requireCurrentUserId()).thenReturn(user.getUserId());
        return user;
    }

    private Image saveAnalyzedImage(long userId, short score) {
        Image image = imageRepository.save(new Image(
                userId,
                "test/check-in-image-" + imageRepository.count(),
                "image/png",
                Instant.now()));
        imageAnalysisRepository.save(new ImageAnalysis(image.id(), score));
        return image;
    }

    private CheckInCreateRequest requestFor(long imageId) {
        return new CheckInCreateRequest(imageId, false, null, (short) 1, List.of());
    }
}
