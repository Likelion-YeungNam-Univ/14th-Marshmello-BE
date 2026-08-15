package Marshmello.MarshmelloWas.domain.checkin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record BodyDiaryRequest(
        @Min(1) @Max(8) short bodyRegion,
        Boolean stretchMark,
        @Size(max = 50) String comment
) {
}
