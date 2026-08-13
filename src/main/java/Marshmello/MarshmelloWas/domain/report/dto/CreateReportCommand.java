package Marshmello.MarshmelloWas.domain.report.dto;

import Marshmello.MarshmelloWas.domain.report.entity.Report;

import java.time.LocalDate;

public record CreateReportCommand(long userId, LocalDate periodStart, LocalDate periodEnd) {
}
