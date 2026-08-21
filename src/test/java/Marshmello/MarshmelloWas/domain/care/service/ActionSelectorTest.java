package Marshmello.MarshmelloWas.domain.care.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ActionSelectorTest {

    @Test
    void selectsOnlyAmongUnratedActionsWhileAnyRemain() {
        ActionSelector selector = new ActionSelector(bound -> bound - 1);

        ActionSelector.Candidate selected = selector.select(List.of(
                new ActionSelector.Candidate(1L, (short) 5),
                new ActionSelector.Candidate(2L, null),
                new ActionSelector.Candidate(3L, null)));

        assertThat(selected.actionId()).isEqualTo(3L);
    }

    @Test
    void usesHelpfulnessScoresAsWeightsAfterAllActionsAreRated() {
        List<ActionSelector.Candidate> candidates = List.of(
                new ActionSelector.Candidate(1L, (short) 1),
                new ActionSelector.Candidate(2L, (short) 2),
                new ActionSelector.Candidate(3L, (short) 5));

        assertThat(new ActionSelector(bound -> 0).select(candidates).actionId()).isEqualTo(1L);
        assertThat(new ActionSelector(bound -> 1).select(candidates).actionId()).isEqualTo(2L);
        assertThat(new ActionSelector(bound -> 7).select(candidates).actionId()).isEqualTo(3L);
    }
}
