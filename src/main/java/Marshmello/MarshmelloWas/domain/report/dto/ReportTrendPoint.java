package Marshmello.MarshmelloWas.domain.report.dto;

import java.time.LocalDate;
import java.util.Objects;

public record ReportTrendPoint(LocalDate checkInDate, short score) {

    public ReportTrendPoint {
        Objects.requireNonNull(checkInDate, "checkInDate");
        if (score < 0) {
            throw new IllegalArgumentException("score must not be negative");
        }
    }
}
