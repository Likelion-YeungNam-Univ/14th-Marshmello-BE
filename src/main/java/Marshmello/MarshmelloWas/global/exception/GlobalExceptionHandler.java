package Marshmello.MarshmelloWas.global.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String INVALID_FIELD_REASON = "올바르지 않은 값입니다.";

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiErrorResponse> handleApiException(ApiException exception) {
        return response(exception.errorCode(), ApiErrorResponse.from(exception));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    ResponseEntity<ApiErrorResponse> handleBindingValidation(BindException exception) {
        List<FieldErrorResponse> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(error -> new FieldErrorResponse(error.getField(), INVALID_FIELD_REASON))
                .toList();
        return response(
                ErrorCode.INVALID_REQUEST,
                ApiErrorResponse.from(ErrorCode.INVALID_REQUEST, fieldErrors));
    }

    @ExceptionHandler({
        ConstraintViolationException.class,
        MethodValidationException.class,
        HandlerMethodValidationException.class
    })
    ResponseEntity<ApiErrorResponse> handleMethodValidation(Exception exception) {
        return response(ErrorCode.INVALID_REQUEST);
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MissingServletRequestPartException.class,
        MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class,
        IllegalArgumentException.class
    })
    ResponseEntity<ApiErrorResponse> handleInvalidRequest(Exception exception) {
        return response(ErrorCode.INVALID_REQUEST);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiErrorResponse> handleUploadOverflow(MaxUploadSizeExceededException exception) {
        return response(ErrorCode.IMAGE_TOO_LARGE);
    }

    @ExceptionHandler(MultipartException.class)
    ResponseEntity<ApiErrorResponse> handleMalformedMultipart(MultipartException exception) {
        return response(ErrorCode.INVALID_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnknown(Exception exception) {
        return response(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ApiErrorResponse> response(ErrorCode errorCode) {
        return response(errorCode, ApiErrorResponse.from(errorCode));
    }

    private ResponseEntity<ApiErrorResponse> response(
            ErrorCode errorCode, ApiErrorResponse body) {
        return ResponseEntity.status(errorCode.status()).body(body);
    }
}
