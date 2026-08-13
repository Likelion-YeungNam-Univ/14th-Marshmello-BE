package Marshmello.MarshmelloWas.domain.checkin.dto;

import Marshmello.MarshmelloWas.domain.checkin.entity.CheckIn;

import java.time.LocalDate;

public record CheckInTrendView(LocalDate checkInDate, short score, boolean achieved) {
}
