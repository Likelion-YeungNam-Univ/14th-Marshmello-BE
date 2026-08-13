package Marshmello.MarshmelloWas.domain.care.model;

public class CareCardGenerationException extends RuntimeException {

    private final Reason reason;

    public CareCardGenerationException(Reason reason) {
        this.reason = reason;
    }

    public CareCardGenerationException(Reason reason, Throwable cause) {
        super(cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }

    public enum Reason {
        UNAVAILABLE,
        TIMEOUT,
        UPSTREAM,
        INVALID_OUTPUT,
        NO_MATCHING_ACTION
    }
}
