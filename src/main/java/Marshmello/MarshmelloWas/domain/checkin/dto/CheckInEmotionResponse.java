package Marshmello.MarshmelloWas.domain.checkin.dto;

import java.time.LocalDate;

public record CheckInEmotionResponse(
        LocalDate date,
        short emotion
) {
}
