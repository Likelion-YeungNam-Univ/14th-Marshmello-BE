package Marshmello.MarshmelloWas.domain.care.repository;

import static org.assertj.core.api.Assertions.assertThat;

import Marshmello.MarshmelloWas.domain.care.entity.Action;
import Marshmello.MarshmelloWas.domain.care.entity.CareCard;
import Marshmello.MarshmelloWas.domain.care.entity.UserActionFeedback;
import Marshmello.MarshmelloWas.domain.care.entity.UserActionFeedbackId;
import Marshmello.MarshmelloWas.domain.checkin.entity.CheckIn;
import Marshmello.MarshmelloWas.domain.checkin.repository.CheckInRepository;
import Marshmello.MarshmelloWas.domain.user.entity.User;
import Marshmello.MarshmelloWas.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CarePersistenceMappingTest {

    private static final String LONGEST_SOURCE =
            "World Gastroenterology Organisation (worldgastroenterology.org)";

    @Autowired
    private ActionRepository actionRepository;

    @Autowired
    private CareCardRepository careCardRepository;

    @Autowired
    private UserActionFeedbackRepository feedbackRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CheckInRepository checkInRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void seedsAllActionsWithoutChangingTheirSource() {
        List<Action> actions = actionRepository.findAll();

        assertThat(actions).hasSize(84);
        assertThat(actions)
                .allSatisfy(action -> assertThat(action.getActionScore()).isBetween((short) 0, (short) 8));
        assertThat(actions).extracting(Action::getSource).contains(LONGEST_SOURCE);
    }

    @Test
    void keepsFeedbackPerUserAndOverwritesOnlyThatUsersScore() {
        Action action = actionRepository.findAll().get(0);
        User firstUser = userRepository.save(new User("first", LocalDate.of(2027, 1, 1)));
        User secondUser = userRepository.save(new User("second", LocalDate.of(2027, 2, 1)));
        UserActionFeedbackId firstId = new UserActionFeedbackId(firstUser.getUserId(), action.getActionId());
        UserActionFeedbackId secondId = new UserActionFeedbackId(secondUser.getUserId(), action.getActionId());
        feedbackRepository.save(new UserActionFeedback(firstId, (short) 2));
        feedbackRepository.save(new UserActionFeedback(secondId, (short) 5));

        feedbackRepository.findById(firstId).orElseThrow().updateHelpfulnessScore((short) 4);
        feedbackRepository.flush();
        entityManager.clear();

        assertThat(feedbackRepository.findById(firstId).orElseThrow().getHelpfulnessScore()).isEqualTo((short) 4);
        assertThat(feedbackRepository.findById(secondId).orElseThrow().getHelpfulnessScore()).isEqualTo((short) 5);
    }

    @Test
    void persistsCareCardSourceAndCreatedDate() {
        User user = userRepository.save(new User("owner", LocalDate.of(2027, 3, 1)));
        CheckIn checkIn = checkInRepository.save(new CheckIn(
                false,
                LocalDate.of(2026, 8, 14),
                null,
                (short) 1,
                user.getUserId()
        ));
        Action action = actionRepository.findAll().stream()
                .filter(candidate -> candidate.getSource().equals(LONGEST_SOURCE))
                .findFirst()
                .orElseThrow();
        LocalDate createdDate = LocalDate.of(2026, 8, 14);
        CareCard careCard = careCardRepository.save(new CareCard(
                "행동 이름",
                "행동 이유",
                action.getSource(),
                checkIn.getCheckInId(),
                action,
                createdDate
        ));
        careCardRepository.flush();
        entityManager.clear();

        CareCard saved = careCardRepository.findById(careCard.getCareCardId()).orElseThrow();
        assertThat(saved.getSource()).isEqualTo(LONGEST_SOURCE);
        assertThat(saved.getCreatedDate()).isEqualTo(createdDate);
    }
}
