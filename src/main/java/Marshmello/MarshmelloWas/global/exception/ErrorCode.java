package Marshmello.MarshmelloWas.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청이 올바르지 않습니다.", false),
    INVALID_REPORT_PERIOD(HttpStatus.BAD_REQUEST, "보고서 기간이 올바르지 않습니다.", false),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.", false),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.", false),
    CHECK_IN_NOT_FOUND(HttpStatus.NOT_FOUND, "체크인을 찾을 수 없습니다.", false),
    CARE_CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "케어 카드를 찾을 수 없습니다.", false),
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "보고서를 찾을 수 없습니다.", false),
    IMAGE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "이미지 크기가 허용 범위를 초과했습니다.", false),
    IMAGE_ANALYSIS_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "이미지를 분석할 수 없습니다.", false),
    REPORT_SOURCE_EMPTY(HttpStatus.UNPROCESSABLE_ENTITY, "보고서에 사용할 체크인이 없습니다.", false),
    ACTION_INVARIANT_VIOLATION(HttpStatus.INTERNAL_SERVER_ERROR, "케어 카드를 생성할 수 없습니다.", false),
    CARE_CARD_GENERATION_FAILED(HttpStatus.BAD_GATEWAY, "케어 카드 생성에 실패했습니다.", true),
    REPORT_GENERATION_FAILED(HttpStatus.BAD_GATEWAY, "보고서 생성에 실패했습니다.", true),
    IMAGE_ANALYZER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "이미지 분석 서비스를 사용할 수 없습니다.", true),
    AI_PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI 서비스를 사용할 수 없습니다.", true),
    AI_GENERATION_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "AI 응답 시간이 초과되었습니다.", true),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.", false);

    private final HttpStatus status;
    private final String message;
    private final boolean retryable;

    ErrorCode(HttpStatus status, String message, boolean retryable) {
        this.status = status;
        this.message = message;
        this.retryable = retryable;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }

    public boolean retryable() {
        return retryable;
    }
}
