package Marshmello.MarshmelloWas.domain.checkin.dto;

import Marshmello.MarshmelloWas.domain.checkin.entity.CheckIn;

import java.time.LocalDate;
import java.util.List;

public record CheckInCommand(
        long userId,
        boolean achieved,
        LocalDate checkInDate,
        String diary,
        short emotion,
        byte[] imageData,
        List<BodyDiaryCommand> bodyDiaries
) {
    public CheckInCommand {
        imageData = imageData.clone();
        bodyDiaries = List.copyOf(bodyDiaries);
    }

    @Override
    public byte[] imageData() {
        return imageData.clone();
    }
}
