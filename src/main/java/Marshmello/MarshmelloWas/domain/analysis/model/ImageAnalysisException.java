package Marshmello.MarshmelloWas.domain.analysis.model;

public class ImageAnalysisException extends RuntimeException {

    private final Reason reason;

    public ImageAnalysisException(Reason reason) {
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }

    public enum Reason {
        UNAVAILABLE,
        UNSCORABLE
    }
}
