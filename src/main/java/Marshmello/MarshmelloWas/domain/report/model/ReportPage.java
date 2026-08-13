package Marshmello.MarshmelloWas.domain.report.model;

import Marshmello.MarshmelloWas.domain.report.entity.Report;

import java.util.List;

public record ReportPage<T>(List<T> content, long totalElements, int totalPages) {

    public ReportPage {
        content = List.copyOf(content);
    }
}
