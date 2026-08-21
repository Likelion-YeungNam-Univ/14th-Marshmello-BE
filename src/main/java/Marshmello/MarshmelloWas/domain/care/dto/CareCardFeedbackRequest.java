package Marshmello.MarshmelloWas.domain.care.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CareCardFeedbackRequest(
        @Min(1) @Max(5) short helpfulnessScore
) {
}
