package Marshmello.MarshmelloWas.domain.care.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import Marshmello.MarshmelloWas.domain.checkin.entity.ImageAnalysis;
import Marshmello.MarshmelloWas.domain.checkin.repository.ImageAnalysisRepository;
import Marshmello.MarshmelloWas.domain.auth.port.CurrentUserIdProvider;
import Marshmello.MarshmelloWas.domain.care.dto.CareCardCreationResult;
import Marshmello.MarshmelloWas.domain.care.dto.CareCardGenerationRequest;
import Marshmello.MarshmelloWas.domain.care.entity.Action;
import Marshmello.MarshmelloWas.domain.care.entity.CareCard;
import Marshmello.MarshmelloWas.domain.care.entity.UserActionFeedbackId;
import Marshmello.MarshmelloWas.domain.care.port.CareCardGenerator;
import Marshmello.MarshmelloWas.domain.care.port.CareCardGenerator.CareCardGeneratedText;
import Marshmello.MarshmelloWas.domain.care.port.CareCardGenerator.CareCardGenerationException;
import Marshmello.MarshmelloWas.domain.care.repository.ActionRepository;
import Marshmello.MarshmelloWas.domain.care.repository.CareCardRepository;
import Marshmello.MarshmelloWas.domain.care.repository.UserActionFeedbackRepository;
import Marshmello.MarshmelloWas.domain.checkin.entity.CheckIn;
import Marshmello.MarshmelloWas.domain.checkin.entity.Image;
import Marshmello.MarshmelloWas.domain.checkin.repository.CheckInRepository;
import Marshmello.MarshmelloWas.domain.checkin.repository.ImageRepository;
import Marshmello.MarshmelloWas.domain.user.entity.User;
import Marshmello.MarshmelloWas.domain.user.repository.UserRepository;
import Marshmello.MarshmelloWas.global.exception.ApiException;
import Marshmello.MarshmelloWas.global.exception.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicLong;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@SpringBootTest
class CareCardServiceIntegrationTest {

    private static final LocalDate CREATED_DATE = LocalDate.of(2026, 8, 14);

    @Autowired
    private CheckInRepository checkInRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ImageAnalysisRepository imageAnalysisRepository;

    @Autowired
    private ActionRepository actionRepository;

    @Autowired
    private CareCardRepository careCardRepository;

    @Autowired
    private UserActionFeedbackRepository feedbackRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private final AtomicLong currentUserId = new AtomicLong();
    private final CareCardGenerator generator = mock(CareCardGenerator.class);
    private CareCardService careCardService;

    @BeforeEach
    void setUp() {
        CurrentUserIdProvider userIdProvider = currentUserId::get;
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-14T03:00:00Z"),
                ZoneId.of("Asia/Seoul"));
        careCardService = new CareCardService(
                checkInRepository,
                imageRepository,
                imageAnalysisRepository,
                actionRepository,
                careCardRepository,
                feedbackRepository,
                userIdProvider,
                generator,
                new ActionSelector(bound -> 0),
                transactionManager,
                clock);
    }

    @AfterEach
    void cleanUp() {
        feedbackRepository.deleteAll();
        careCardRepository.deleteAll();
        imageAnalysisRepository.deleteAll();
        imageRepository.deleteAll();
        checkInRepository.deleteAll();
        userRepository.deleteAll();
        reset(generator);
    }

    @Test
    void createsCardAfterCheckInAndReturnsExistingCardWithoutCallingAiAgain() {
        CheckIn checkIn = saveCheckInContext((short) 8);
        Action expectedAction = actionRepository.findByActionScoreOrderByActionIdAsc((short) 8).get(0);
        when(generator.generate(any())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return new CareCardGeneratedText("진료 필요 신호 확인", "위험 증상을 확인하고 진료를 안내합니다.");
        });

        CareCardCreationResult created = careCardService.create(checkIn.id());
        CareCardCreationResult existing = careCardService.create(checkIn.id());

        assertThat(created.created()).isTrue();
        assertThat(created.careCard().category()).isEqualTo(expectedAction.getCategory());
        assertThat(created.careCard().source()).isEqualTo(expectedAction.getSource());
        assertThat(created.careCard().createdDate()).isEqualTo(CREATED_DATE);
        assertThat(existing.created()).isFalse();
        assertThat(existing.careCard()).isEqualTo(created.careCard());
        verify(generator).generate(new CareCardGenerationRequest(
                expectedAction.getCategory(),
                expectedAction.getGuideText()));
    }

    @Test
    void overwritesCurrentUsersFeedbackForTheGeneratedAction() {
        CheckIn checkIn = saveCheckInContext((short) 8);
        when(generator.generate(any())).thenReturn(new CareCardGeneratedText("행동", "이유"));
        CareCardCreationResult result = careCardService.create(checkIn.id());
        Long actionId = careCardRepository.findById(result.careCard().careCardId())
                .orElseThrow()
                .getAction()
                .getActionId();
        UserActionFeedbackId feedbackId = new UserActionFeedbackId(currentUserId.get(), actionId);

        careCardService.updateFeedback(result.careCard().careCardId(), (short) 2);
        careCardService.updateFeedback(result.careCard().careCardId(), (short) 5);

        assertThat(feedbackRepository.findById(feedbackId).orElseThrow().getHelpfulnessScore())
                .isEqualTo((short) 5);
        assertThat(feedbackRepository.count()).isOne();
    }

    @Test
    void rejectsAnotherUsersCheckInWithoutCallingAi() {
        CheckIn checkIn = saveCheckInContext((short) 8);
        currentUserId.set(userRepository.save(new User("other", null)).getUserId());

        assertThatThrownBy(() -> careCardService.create(checkIn.id()))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.CHECK_IN_NOT_FOUND));
        verifyNoInteractions(generator);
    }

    @Test
    void rejectsAnotherUsersFeedback() {
        CheckIn checkIn = saveCheckInContext((short) 8);
        when(generator.generate(any())).thenReturn(new CareCardGeneratedText("행동", "이유"));
        CareCardCreationResult result = careCardService.create(checkIn.id());
        currentUserId.set(userRepository.save(new User("other", null)).getUserId());

        assertThatThrownBy(() -> careCardService.updateFeedback(result.careCard().careCardId(), (short) 5))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.CARE_CARD_NOT_FOUND));
        assertThat(feedbackRepository.count()).isZero();
    }

    @Test
    void mapsGeneratorTimeoutToApiError() {
        CheckIn checkIn = saveCheckInContext((short) 8);
        when(generator.generate(any())).thenThrow(new CareCardGenerationException(
                CareCardGenerationException.Reason.TIMEOUT));

        assertThatThrownBy(() -> careCardService.create(checkIn.id()))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.AI_GENERATION_TIMEOUT));
        assertThat(careCardRepository.count()).isZero();
    }

    @Test
    void getsLatestCurrentUsersCardByDateBeforeIdWithOneBoundedFetchJoinedQuery() {
        User owner = userRepository.save(new User("latest-owner", null));
        currentUserId.set(owner.getUserId());
        CareCard newerOwnersCard = saveCareCard(
                owner.getUserId(), LocalDate.of(2026, 8, 14), LocalDate.of(2026, 8, 15), "newer");
        CareCard olderOwnersCard = saveCareCard(
                owner.getUserId(), LocalDate.of(2026, 8, 13), LocalDate.of(2026, 8, 14), "older");
        User other = userRepository.save(new User("latest-other", null));
        CareCard newerOtherUsersCard = saveCareCard(
                other.getUserId(), LocalDate.of(2026, 8, 14), LocalDate.of(2026, 8, 16), "other");
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        boolean statisticsEnabled = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        try {
            var response = careCardService.getLatest();

            assertThat(olderOwnersCard.getCareCardId()).isGreaterThan(newerOwnersCard.getCareCardId());
            assertThat(response.careCardId()).isEqualTo(newerOwnersCard.getCareCardId());
            assertThat(response.careCardId()).isNotEqualTo(newerOtherUsersCard.getCareCardId());
            assertThat(response.category()).isEqualTo(newerOwnersCard.getAction().getCategory());
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        } finally {
            statistics.clear();
            statistics.setStatisticsEnabled(statisticsEnabled);
        }
    }

    @Test
    void getsLatestCurrentUsersCardByLargerIdWhenDatesAreEqual() {
        User owner = userRepository.save(new User("latest-tie", null));
        currentUserId.set(owner.getUserId());
        CareCard firstCard = saveCareCard(
                owner.getUserId(), LocalDate.of(2026, 8, 14), LocalDate.of(2026, 8, 15), "first");
        CareCard secondCard = saveCareCard(
                owner.getUserId(), LocalDate.of(2026, 8, 16), LocalDate.of(2026, 8, 15), "second");

        var response = careCardService.getLatest();

        assertThat(secondCard.getCareCardId()).isGreaterThan(firstCard.getCareCardId());
        assertThat(response.careCardId()).isEqualTo(secondCard.getCareCardId());
    }

    @Test
    void rejectsLatestLookupWhenCurrentUserHasNoCareCard() {
        User userWithoutCard = userRepository.save(new User("latest-empty", null));
        currentUserId.set(userWithoutCard.getUserId());

        assertThatThrownBy(() -> careCardService.getLatest())
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.CARE_CARD_NOT_FOUND));
    }

    private CheckIn saveCheckInContext(short analysisScore) {
        User user = userRepository.save(new User("owner", null));
        currentUserId.set(user.getUserId());
        CheckIn checkIn = checkInRepository.save(new CheckIn(
                false,
                LocalDate.of(2026, 8, 14),
                null,
                (short) 1,
                user.getUserId()));
        Image image = new Image(user.getUserId(), "test/care-image", "image/png", Instant.now());
        image.attachTo(checkIn, user.getUserId());
        imageRepository.save(image);
        imageAnalysisRepository.save(new ImageAnalysis(image.id(), analysisScore));
        return checkIn;
    }

    private CareCard saveCareCard(long userId, LocalDate checkInDate, LocalDate createdDate, String suffix) {
        CheckIn checkIn = checkInRepository.save(new CheckIn(
                false,
                checkInDate,
                null,
                (short) 1,
                userId));
        Action action = actionRepository.findByActionScoreOrderByActionIdAsc((short) 8).get(0);
        return careCardRepository.saveAndFlush(new CareCard(
                "action-" + suffix,
                "reason-" + suffix,
                action.getSource(),
                checkIn.id(),
                action,
                createdDate));
    }
}
