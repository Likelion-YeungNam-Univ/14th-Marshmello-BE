package Marshmello.MarshmelloWas.domain.checkin.port;

public interface ImageAnalyzer {

    AnalysisResult analyze(byte[] image);

    record AnalysisResult(boolean detected, Short score) {

        public AnalysisResult {
            if (detected && (score == null || score < 1 || score > 8)) {
                throw new IllegalArgumentException("Detected image score must be between 1 and 8");
            }
            if (!detected && score != null) {
                throw new IllegalArgumentException("Undetected image cannot have a score");
            }
        }

        public static AnalysisResult detected(short score) {
            return new AnalysisResult(true, score);
        }

        public static AnalysisResult notDetected() {
            return new AnalysisResult(false, null);
        }
    }

    final class ImageAnalysisException extends RuntimeException {

        private final Reason reason;

        public ImageAnalysisException(Reason reason) {
            this.reason = reason;
        }

        public ImageAnalysisException(Reason reason, Throwable cause) {
            super(cause);
            this.reason = reason;
        }

        public Reason reason() {
            return reason;
        }

        public enum Reason {
            INVALID_IMAGE,
            FAILED,
            UNAVAILABLE
        }
    }
}
