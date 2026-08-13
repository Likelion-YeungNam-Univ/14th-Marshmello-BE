package Marshmello.MarshmelloWas.domain.care.model;

import java.util.List;

public record CareCardGenerationContext(short score, List<ActionCandidate> candidates) {

    public CareCardGenerationContext {
        candidates = List.copyOf(candidates);
    }
}
