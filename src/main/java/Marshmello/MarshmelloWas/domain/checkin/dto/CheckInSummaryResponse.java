package Marshmello.MarshmelloWas.domain.checkin.dto;

import java.time.LocalDate;

public record CheckInSummaryResponse(
        long checkInId,
        long imageId,
        LocalDate checkInDate,
        boolean achieved,
        short emotion
) {
}
