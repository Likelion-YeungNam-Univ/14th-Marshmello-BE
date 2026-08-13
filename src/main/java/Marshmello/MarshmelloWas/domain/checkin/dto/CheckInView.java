package Marshmello.MarshmelloWas.domain.checkin.dto;

import Marshmello.MarshmelloWas.domain.checkin.entity.CheckIn;

import java.time.LocalDate;
import java.util.List;

public record CheckInView(
        long checkInId,
        boolean achieved,
        LocalDate checkInDate,
        String diary,
        short emotion,
        short score,
        List<BodyDiaryView> bodyDiaries
) {
    public CheckInView {
        bodyDiaries = List.copyOf(bodyDiaries);
    }
}
