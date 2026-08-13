package Marshmello.MarshmelloWas.domain.care.model;

import java.util.Objects;

public record CareCardGeneratedText(String actionName, String actionReason) {

    public CareCardGeneratedText {
        actionName = normalized(actionName, "actionName");
        actionReason = normalized(actionReason, "actionReason");
        if (actionName.length() > 50) {
            throw new IllegalArgumentException("actionName must not exceed 50 characters");
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
