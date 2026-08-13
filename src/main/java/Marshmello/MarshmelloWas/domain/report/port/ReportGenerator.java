package Marshmello.MarshmelloWas.domain.report.port;

import Marshmello.MarshmelloWas.domain.report.dto.ReportGenerationRequest;
import java.util.Objects;

public interface ReportGenerator {

    GeneratedContent generate(ReportGenerationRequest request);

    record GeneratedContent(String content) {

        private static final int MAX_CONTENT_LENGTH = 8_000;

        public GeneratedContent {
            content = Objects.requireNonNull(content, "content").trim();
            if (content.isEmpty()) {
                throw new IllegalArgumentException("content must not be blank");
            }
            if (content.length() > MAX_CONTENT_LENGTH) {
                throw new IllegalArgumentException("content must not exceed 8000 characters");
            }
        }
    }

    class GenerationException extends RuntimeException {

        private final Reason reason;

        public GenerationException(Reason reason) {
            this.reason = Objects.requireNonNull(reason, "reason");
        }

        public GenerationException(Reason reason, Throwable cause) {
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
}
