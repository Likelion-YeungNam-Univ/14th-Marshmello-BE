package Marshmello.MarshmelloWas.domain.care.entity;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CareEntityTest {

    @Test
    void validatesActionScoreRange() {
        assertThatThrownBy(() -> new Action((short) -1, "category", "guide", "source"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Action((short) 9, "category", "guide", "source"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validatesHelpfulnessScoreRange() {
        UserActionFeedbackId id = new UserActionFeedbackId(1L, 1L);

        assertThatThrownBy(() -> new UserActionFeedback(id, (short) 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new UserActionFeedback(id, (short) 6))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
