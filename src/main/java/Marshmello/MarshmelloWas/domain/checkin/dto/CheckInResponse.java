package Marshmello.MarshmelloWas.domain.checkin.dto;

import java.time.LocalDate;
import java.util.List;

public record CheckInResponse(
        long checkInId,
        long imageId,
        boolean achieved,
        LocalDate checkInDate,
        String diary,
        short emotion,
        List<BodyDiaryResponse> bodyDiaries
) {
    public CheckInResponse {
        bodyDiaries = List.copyOf(bodyDiaries);
    }
}
