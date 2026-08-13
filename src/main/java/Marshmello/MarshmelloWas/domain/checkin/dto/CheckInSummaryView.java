package Marshmello.MarshmelloWas.domain.checkin.dto;

import Marshmello.MarshmelloWas.domain.checkin.entity.CheckIn;

import java.time.LocalDate;

public record CheckInSummaryView(long checkInId, boolean achieved, LocalDate checkInDate, short score) {
}
