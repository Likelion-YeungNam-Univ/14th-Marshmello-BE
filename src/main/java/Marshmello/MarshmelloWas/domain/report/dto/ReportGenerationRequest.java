package Marshmello.MarshmelloWas.domain.report.dto;

import java.util.List;
import java.util.Objects;

public record ReportGenerationRequest(List<ReportTrendPoint> trendPoints) {

    public ReportGenerationRequest {
        trendPoints = List.copyOf(Objects.requireNonNull(trendPoints, "trendPoints"));
        if (trendPoints.size() < 2) {
            throw new IllegalArgumentException("at least two trend points are required");
        }
    }
}
