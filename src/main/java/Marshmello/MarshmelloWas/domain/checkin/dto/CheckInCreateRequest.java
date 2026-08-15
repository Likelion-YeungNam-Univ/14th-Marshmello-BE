package Marshmello.MarshmelloWas.domain.checkin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CheckInCreateRequest(
        @NotNull @Positive Long imageId,
        boolean achieved,
        @Size(max = 255) String diary,
        @Min(1) @Max(4) short emotion,
        @NotNull List<@Valid BodyDiaryRequest> bodyDiaries
) {
    public CheckInCreateRequest {
        bodyDiaries = List.copyOf(bodyDiaries);
    }
}
