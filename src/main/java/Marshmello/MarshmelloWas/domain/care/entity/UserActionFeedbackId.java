package Marshmello.MarshmelloWas.domain.care.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import lombok.Getter;

@Getter
@Embeddable
public class UserActionFeedbackId implements Serializable {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "action_id", nullable = false)
    private Long actionId;

    protected UserActionFeedbackId() {
    }

    public UserActionFeedbackId(Long userId, Long actionId) {
        this.userId = Objects.requireNonNull(userId, "userId");
        this.actionId = Objects.requireNonNull(actionId, "actionId");
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UserActionFeedbackId that)) {
            return false;
        }
        return Objects.equals(userId, that.userId) && Objects.equals(actionId, that.actionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, actionId);
    }
}
