package Marshmello.MarshmelloWas.global.exception;

import java.util.List;

public record ApiErrorResponse(
        ErrorCode code,
        String message,
        boolean retryable,
        List<FieldErrorResponse> fieldErrors,
        CommittedCheckInResponse committedCheckIn) {

    public ApiErrorResponse {
        fieldErrors = List.copyOf(fieldErrors);
    }

    public static ApiErrorResponse from(ApiException exception) {
        return from(exception.errorCode(), exception.fieldErrors(), exception.committedCheckIn());
    }

    public static ApiErrorResponse from(ErrorCode errorCode) {
        return from(errorCode, List.of(), null);
    }

    public static ApiErrorResponse from(ErrorCode errorCode, List<FieldErrorResponse> fieldErrors) {
        return from(errorCode, fieldErrors, null);
    }

    private static ApiErrorResponse from(
            ErrorCode errorCode,
            List<FieldErrorResponse> fieldErrors,
            CommittedCheckInResponse committedCheckIn) {
        return new ApiErrorResponse(
                errorCode,
                errorCode.message(),
                errorCode.retryable(),
                fieldErrors,
                committedCheckIn);
    }
}
