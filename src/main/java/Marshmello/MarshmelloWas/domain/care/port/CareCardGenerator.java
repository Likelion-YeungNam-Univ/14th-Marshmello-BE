package Marshmello.MarshmelloWas.domain.care.port;

import Marshmello.MarshmelloWas.domain.care.dto.CareCardGenerationRequest;
import java.util.Objects;

public interface CareCardGenerator {

    CareCardGeneratedText generate(CareCardGenerationRequest request);

    record CareCardGeneratedText(String actionName, String actionReason) {

        public CareCardGeneratedText {
            actionName = normalized(actionName, "actionName");
            actionReason = normalized(actionReason, "actionReason");
            if (actionName.length() > 30) {
                throw new IllegalArgumentException("actionName must not exceed 30 characters");
            }
            if (actionReason.length() > 150) {
                throw new IllegalArgumentException("actionReason must not exceed 150 characters");
            }
        }

        private static String normalized(String value, String fieldName) {
            String normalizedValue = Objects.requireNonNull(value, fieldName).trim();
            if (normalizedValue.isEmpty()) {
                throw new IllegalArgumentException(fieldName + " must not be blank");
            }
            return normalizedValue;
        }
    }

    class CareCardGenerationException extends RuntimeException {

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
            AUTHENTICATION,
            ACCESS_DENIED,
            MODEL_UNAVAILABLE,
            QUOTA_EXCEEDED,
            RATE_LIMITED,
            REQUEST_REJECTED,
            TIMEOUT,
            UPSTREAM,
            INVALID_OUTPUT,
            NO_MATCHING_ACTION
        }
    }
}
