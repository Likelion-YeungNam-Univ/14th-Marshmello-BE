package Marshmello.MarshmelloWas.domain.care.service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntUnaryOperator;
import org.springframework.stereotype.Component;

@Component
class ActionSelector {

    private final IntUnaryOperator randomIndex;

    ActionSelector() {
        this(bound -> ThreadLocalRandom.current().nextInt(bound));
    }

    ActionSelector(IntUnaryOperator randomIndex) {
        this.randomIndex = Objects.requireNonNull(randomIndex, "randomIndex");
    }

    Candidate select(List<Candidate> candidates) {
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("action candidates must not be empty");
        }

        List<Candidate> unrated = candidates.stream()
                .filter(candidate -> candidate.helpfulnessScore() == null)
                .toList();
        if (!unrated.isEmpty()) {
            return unrated.get(nextIndex(unrated.size()));
        }

        int totalWeight = candidates.stream()
                .mapToInt(candidate -> candidate.helpfulnessScore())
                .sum();
        int selectedWeight = nextIndex(totalWeight);
        int cumulativeWeight = 0;
        for (Candidate candidate : candidates) {
            cumulativeWeight += candidate.helpfulnessScore();
            if (selectedWeight < cumulativeWeight) {
                return candidate;
            }
        }
        throw new IllegalStateException("weighted action selection failed");
    }

    private int nextIndex(int bound) {
        int selected = randomIndex.applyAsInt(bound);
        if (selected < 0 || selected >= bound) {
            throw new IllegalStateException("random index is outside its bound");
        }
        return selected;
    }

    record Candidate(long actionId, Short helpfulnessScore) {

        Candidate {
            if (helpfulnessScore != null && (helpfulnessScore < 1 || helpfulnessScore > 5)) {
                throw new IllegalArgumentException("helpfulnessScore must be between 1 and 5");
            }
        }
    }
}
