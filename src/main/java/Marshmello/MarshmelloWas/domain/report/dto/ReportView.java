package Marshmello.MarshmelloWas.domain.report.dto;

import Marshmello.MarshmelloWas.domain.report.entity.Report;

import java.time.LocalDate;

public record ReportView(long reportId, LocalDate periodStart, LocalDate periodEnd, String content) {
}
