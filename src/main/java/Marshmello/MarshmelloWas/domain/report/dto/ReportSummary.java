package Marshmello.MarshmelloWas.domain.report.dto;

import Marshmello.MarshmelloWas.domain.report.entity.Report;

import java.time.LocalDate;

public record ReportSummary(long reportId, LocalDate periodStart, LocalDate periodEnd) {
}
