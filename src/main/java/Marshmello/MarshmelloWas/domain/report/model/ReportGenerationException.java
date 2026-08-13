package Marshmello.MarshmelloWas.domain.report.model;

import Marshmello.MarshmelloWas.domain.report.entity.Report;

import java.util.Objects;

public class ReportGenerationException extends RuntimeException {

    private final Reason reason;

    public ReportGenerationException(Reason reason) {
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    public ReportGenerationException(Reason reason, Throwable cause) {
        super(cause);
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    public Reason getReason() {
        return reason;
    }

    public enum Reason {
        UNAVAILABLE,
        TIMEOUT,
        UPSTREAM,
        INVALID_OUTPUT
    }
}
