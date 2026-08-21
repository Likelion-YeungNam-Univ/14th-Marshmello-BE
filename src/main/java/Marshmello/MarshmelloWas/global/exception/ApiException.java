package Marshmello.MarshmelloWas.global.exception;

import java.util.List;

public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<FieldErrorResponse> fieldErrors;
    private final CommittedCheckInResponse committedCheckIn;

    public ApiException(ErrorCode errorCode) {
        this(errorCode, List.of(), null);
    }

    public ApiException(ErrorCode errorCode, CommittedCheckInResponse committedCheckIn) {
        this(errorCode, List.of(), committedCheckIn);
    }

    public ApiException(ErrorCode errorCode, List<FieldErrorResponse> fieldErrors) {
        this(errorCode, fieldErrors, null);
    }

    public ApiException(
            ErrorCode errorCode,
            List<FieldErrorResponse> fieldErrors,
            CommittedCheckInResponse committedCheckIn) {
        super(errorCode.message());
        this.errorCode = errorCode;
        this.fieldErrors = List.copyOf(fieldErrors);
        this.committedCheckIn = committedCheckIn;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public List<FieldErrorResponse> fieldErrors() {
        return fieldErrors;
    }

    public CommittedCheckInResponse committedCheckIn() {
        return committedCheckIn;
    }
}
