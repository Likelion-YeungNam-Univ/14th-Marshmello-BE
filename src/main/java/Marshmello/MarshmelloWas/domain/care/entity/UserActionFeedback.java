package Marshmello.MarshmelloWas.domain.care.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.Getter;

@Getter
@Entity
@Table(name = "user_action_feedback")
public class UserActionFeedback {

    @EmbeddedId
    private UserActionFeedbackId feedbackId;

    @Column(name = "helpfulness_score", nullable = false)
    private short helpfulnessScore;

    protected UserActionFeedback() {
    }

    public UserActionFeedback(UserActionFeedbackId feedbackId, short helpfulnessScore) {
        this.feedbackId = Objects.requireNonNull(feedbackId, "feedbackId");
        updateHelpfulnessScore(helpfulnessScore);
    }

    public void updateHelpfulnessScore(short helpfulnessScore) {
        if (helpfulnessScore < 1 || helpfulnessScore > 5) {
            throw new IllegalArgumentException("helpfulnessScore must be between 1 and 5");
        }
        this.helpfulnessScore = helpfulnessScore;
    }

}
