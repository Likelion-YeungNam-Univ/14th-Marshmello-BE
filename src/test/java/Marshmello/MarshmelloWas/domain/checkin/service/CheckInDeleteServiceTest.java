package Marshmello.MarshmelloWas.domain.checkin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import Marshmello.MarshmelloWas.domain.auth.port.CurrentUserIdProvider;
import Marshmello.MarshmelloWas.domain.checkin.entity.BodyDiary;
import Marshmello.MarshmelloWas.domain.checkin.entity.BodyRegion;
import Marshmello.MarshmelloWas.domain.checkin.entity.CheckIn;
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
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class CheckInDeleteServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 15);

    @Autowired
    private CheckInService service;

    @Autowired
    private CheckInRepository checkInRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ImageAnalysisRepository imageAnalysisRepository;

    @Autowired
    private BodyDiaryRepository bodyDiaryRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean(name = "oidcCurrentUserIdProvider")
    private CurrentUserIdProvider currentUserIdProvider;

    @AfterEach
    void cleanUp() {
        bodyDiaryRepository.deleteAll();
        imageAnalysisRepository.deleteAll();
        imageRepository.deleteAll();
        checkInRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void deletesCurrentUsersCheckInWhenIdAndDateMatch() {
        User user = saveCurrentUser("delete-owner");
        CheckIn checkIn = saveCheckIn(user, DATE);

        service.delete(checkIn.id(), DATE);

        assertThat(checkInRepository.findById(checkIn.id())).isEmpty();
    }

    @Test
    void deletesCheckInAndAllChildrenWhenIdAndDateMatch() {
        User user = saveCurrentUser("cascade-owner");
        CheckIn checkIn = saveCheckIn(user, DATE);
        Image image = new Image(
                user.getUserId(),
                "users/" + user.getUserId() + "/check-in-images/cascade-test",
                "image/png",
                Instant.parse("2026-08-15T03:00:00Z"));
        image.attachTo(checkIn, user.getUserId());
        imageRepository.save(image);
        ImageAnalysis imageAnalysis = imageAnalysisRepository.save(
                new ImageAnalysis(image.id(), (short) 6));
        BodyDiary bodyDiary = bodyDiaryRepository.save(
                new BodyDiary(BodyRegion.CHEST, checkIn, true, "cascade-test"));

        service.delete(checkIn.id(), DATE);

        assertThat(checkInRepository.findById(checkIn.id())).isEmpty();
        assertThat(imageRepository.findById(image.id())).isEmpty();
        assertThat(imageAnalysisRepository.findById(imageAnalysis.imageId())).isEmpty();
        assertThat(bodyDiaryRepository.findById(bodyDiary.id())).isEmpty();
    }

    @Test
    void hidesAnotherUsersCheckInAsNotFoundWithoutDeletingIt() {
        saveCurrentUser("requesting-user");
        CheckIn checkIn = saveCheckIn(userRepository.save(new User("other-user", null)), DATE);

        assertThatThrownBy(() -> service.delete(checkIn.id(), DATE))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.CHECK_IN_NOT_FOUND));

        assertThat(checkInRepository.findById(checkIn.id())).isPresent();
    }

    @Test
    void hidesAUsersCheckInWhenTheRequestedDateDoesNotMatch() {
        User user = saveCurrentUser("date-owner");
        CheckIn checkIn = saveCheckIn(user, DATE);

        assertThatThrownBy(() -> service.delete(checkIn.id(), DATE.plusDays(1)))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.CHECK_IN_NOT_FOUND));

        assertThat(checkInRepository.findById(checkIn.id())).isPresent();
    }

    @Test
    void reportsANonexistentCheckInAsNotFound() {
        saveCurrentUser("missing-user");

        assertThatThrownBy(() -> service.delete(999L, DATE))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.CHECK_IN_NOT_FOUND));
    }

    private User saveCurrentUser(String subject) {
        User user = userRepository.save(new User(subject, null));
        when(currentUserIdProvider.requireCurrentUserId()).thenReturn(user.getUserId());
        return user;
    }

    private CheckIn saveCheckIn(User user, LocalDate date) {
        return checkInRepository.saveAndFlush(new CheckIn(false, date, null, (short) 1, user.getUserId()));
    }
}
