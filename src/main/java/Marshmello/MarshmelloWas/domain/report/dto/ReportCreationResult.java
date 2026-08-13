package Marshmello.MarshmelloWas.domain.report.dto;

import Marshmello.MarshmelloWas.domain.report.entity.Report;

public record ReportCreationResult(ReportView report, boolean created) {
}
