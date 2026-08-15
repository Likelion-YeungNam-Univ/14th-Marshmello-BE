package Marshmello.MarshmelloWas.domain.report.dto;

import java.time.YearMonth;

public record ReportResponse(
        long reportId,
        YearMonth yearMonth,
        String content
) {
}
