package Marshmello.MarshmelloWas.domain.report.dto;

import Marshmello.MarshmelloWas.domain.report.entity.Report;

import java.util.List;

public record ReportGenerationRequest(List<ReportTrendPoint> trendPoints) {

    public ReportGenerationRequest {
        trendPoints = List.copyOf(trendPoints);
    }
}
