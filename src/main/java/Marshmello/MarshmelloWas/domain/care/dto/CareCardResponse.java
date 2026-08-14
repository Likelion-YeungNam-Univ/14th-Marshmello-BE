package Marshmello.MarshmelloWas.domain.care.dto;

import java.time.LocalDate;

public record CareCardResponse(
        Long careCardId,
        Long checkInId,
        String actionName,
        String actionReason,
        String category,
        String source,
        LocalDate createdDate
) {
}
