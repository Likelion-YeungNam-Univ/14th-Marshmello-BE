package Marshmello.MarshmelloWas.domain.care.port;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import Marshmello.MarshmelloWas.domain.care.port.CareCardGenerator.CareCardGeneratedText;
import org.junit.jupiter.api.Test;

class CareCardGeneratorTest {

    @Test
    void acceptsMaximumLengthsIncludingSpaces() {
        String actionName = "가 ".repeat(14) + "가나";
        String actionReason = "나 ".repeat(49) + "나나";

        assertThatCode(() -> new CareCardGeneratedText(actionName, actionReason))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsActionNameLongerThanThirtyCharactersIncludingSpaces() {
        String actionName = "가 ".repeat(15) + "가";

        assertThatThrownBy(() -> new CareCardGeneratedText(actionName, "이유"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("actionName must not exceed 30 characters");
    }

    @Test
    void rejectsActionReasonLongerThanOneHundredCharactersIncludingSpaces() {
        String actionReason = "나 ".repeat(50) + "나";

        assertThatThrownBy(() -> new CareCardGeneratedText("행동", actionReason))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("actionReason must not exceed 100 characters");
    }
}
