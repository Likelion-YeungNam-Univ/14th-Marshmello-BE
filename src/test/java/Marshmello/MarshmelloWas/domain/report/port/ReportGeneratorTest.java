package Marshmello.MarshmelloWas.domain.report.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import Marshmello.MarshmelloWas.domain.report.port.ReportGenerator.GeneratedContent;
import org.junit.jupiter.api.Test;

class ReportGeneratorTest {

    @Test
    void acceptsTrimmedContentBetweenSixtyAndSeventyCharacters() {
        assertThat(new GeneratedContent("  " + "가".repeat(60) + "  ").content())
                .hasSize(60);
        assertThat(new GeneratedContent("가".repeat(70)).content())
                .hasSize(70);
    }

    @Test
    void rejectsContentOutsideRequiredLength() {
        assertThatThrownBy(() -> new GeneratedContent("가".repeat(59)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GeneratedContent("가".repeat(71)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
