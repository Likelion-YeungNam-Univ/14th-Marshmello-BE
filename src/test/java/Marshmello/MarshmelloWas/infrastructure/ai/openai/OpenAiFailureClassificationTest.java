package Marshmello.MarshmelloWas.infrastructure.ai.openai;

import static org.assertj.core.api.Assertions.assertThat;

import com.openai.core.http.Headers;
import com.openai.errors.BadRequestException;
import com.openai.errors.InternalServerException;
import com.openai.errors.NotFoundException;
import com.openai.errors.OpenAIException;
import com.openai.errors.OpenAIInvalidDataException;
import com.openai.errors.OpenAIIoException;
import com.openai.errors.PermissionDeniedException;
import com.openai.errors.RateLimitException;
import com.openai.errors.UnauthorizedException;
import com.openai.models.ErrorObject;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class OpenAiFailureClassificationTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("serviceErrors")
    void distinguishesOpenAiServiceFailures(
            String scenario,
            OpenAIException exception,
            String expectedKind
    ) {
        assertThat(OpenAiStructuredResponseSupport.classify(exception).name())
                .as(scenario)
                .isEqualTo(expectedKind);
    }

    @Test
    void distinguishesTimeoutFromOtherIoFailures() {
        OpenAIIoException timeout = new OpenAIIoException(
                "timeout",
                new SocketTimeoutException("test timeout"));
        OpenAIIoException connectionFailure = new OpenAIIoException(
                "connection",
                new IOException("test connection failure"));

        assertThat(OpenAiStructuredResponseSupport.classify(timeout).name()).isEqualTo("TIMEOUT");
        assertThat(OpenAiStructuredResponseSupport.classify(connectionFailure).name()).isEqualTo("UPSTREAM");
    }

    @Test
    void identifiesInvalidSdkResponseData() {
        assertThat(OpenAiStructuredResponseSupport.classify(
                new OpenAIInvalidDataException("invalid response data")).name())
                .isEqualTo("INVALID_OUTPUT");
    }

    private static Stream<Arguments> serviceErrors() {
        return Stream.of(
                Arguments.of(
                        "401 authentication",
                        UnauthorizedException.builder()
                                .headers(headers())
                                .error(error("invalid_api_key", "invalid_request_error"))
                                .build(),
                        "AUTHENTICATION"),
                Arguments.of(
                        "403 access denied",
                        PermissionDeniedException.builder()
                                .headers(headers())
                                .error(error("permission_denied", "invalid_request_error"))
                                .build(),
                        "ACCESS_DENIED"),
                Arguments.of(
                        "404 model unavailable",
                        NotFoundException.builder()
                                .headers(headers())
                                .error(error("model_not_found", "invalid_request_error"))
                                .build(),
                        "MODEL_UNAVAILABLE"),
                Arguments.of(
                        "429 exhausted credits",
                        RateLimitException.builder()
                                .headers(headers())
                                .error(error("credit_balance_exhausted", "insufficient_quota"))
                                .build(),
                        "QUOTA_EXCEEDED"),
                Arguments.of(
                        "429 rate limit",
                        RateLimitException.builder()
                                .headers(headers())
                                .error(error("rate_limit_exceeded", "rate_limit_error"))
                                .build(),
                        "RATE_LIMITED"),
                Arguments.of(
                        "400 rejected request",
                        BadRequestException.builder()
                                .headers(headers())
                                .error(error("invalid_request", "invalid_request_error"))
                                .build(),
                        "REQUEST_REJECTED"),
                Arguments.of(
                        "500 provider failure",
                        InternalServerException.builder()
                                .statusCode(500)
                                .headers(headers())
                                .error(error("server_error", "server_error"))
                                .build(),
                        "UPSTREAM"));
    }

    private static Headers headers() {
        return Headers.builder().put("x-request-id", "req_test").build();
    }

    private static ErrorObject error(String code, String type) {
        return ErrorObject.builder()
                .message("test provider error")
                .code(code)
                .param("model")
                .type(type)
                .build();
    }
}
