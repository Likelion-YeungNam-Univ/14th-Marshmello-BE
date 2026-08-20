package Marshmello.MarshmelloWas.infrastructure.ai.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.errors.OpenAIIoException;
import com.openai.errors.OpenAIException;
import com.openai.errors.OpenAIInvalidDataException;
import com.openai.errors.OpenAIServiceException;
import com.openai.models.responses.ResponseOutputMessage;
import com.openai.models.responses.ResponseStatus;
import com.openai.models.responses.StructuredResponse;
import com.openai.models.responses.StructuredResponseOutputItem;
import com.openai.models.responses.StructuredResponseOutputMessage;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class OpenAiStructuredResponseSupport {

    private static final Set<String> QUOTA_ERROR_CODES = Set.of(
            "credit_balance_exhausted",
            "organization_spend_limit_exceeded",
            "project_spend_limit_exceeded",
            "organization_usage_limit_exceeded"
    );

    private OpenAiStructuredResponseSupport() {
    }

    public static <T> T requireSingleCompletedOutput(StructuredResponse<T> response) {
        if (response.status().isEmpty()
                || response.status().orElseThrow().known() != ResponseStatus.Known.COMPLETED
                || response.incompleteDetails().isPresent()
                || response.error().isPresent()) {
            throw new UpstreamResponseException();
        }

        List<T> outputs = new ArrayList<>(1);
        for (StructuredResponseOutputItem<T> item : response.output()) {
            item.message().ifPresent(message -> collectMessage(message, outputs));
        }
        if (outputs.size() != 1) {
            throw new InvalidOutputException();
        }
        return outputs.get(0);
    }

    private static <T> void collectMessage(
            StructuredResponseOutputMessage<T> message,
            List<T> outputs
    ) {
        if (message.status().known() != ResponseOutputMessage.Status.Known.COMPLETED) {
            throw new UpstreamResponseException();
        }
        for (StructuredResponseOutputMessage.Content<T> content : message.content()) {
            if (content.refusal().isPresent()) {
                throw new UpstreamResponseException();
            }
            content.outputText().ifPresent(outputs::add);
        }
    }

    static boolean isTimeout(OpenAIIoException exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    public static FailureKind classify(OpenAIException exception) {
        if (exception instanceof OpenAIIoException ioException && isTimeout(ioException)) {
            return FailureKind.TIMEOUT;
        }
        if (exception instanceof OpenAIInvalidDataException) {
            return FailureKind.INVALID_OUTPUT;
        }
        if (exception instanceof OpenAIServiceException serviceException) {
            return switch (serviceException.statusCode()) {
                case 400, 422 -> FailureKind.REQUEST_REJECTED;
                case 401 -> FailureKind.AUTHENTICATION;
                case 403 -> FailureKind.ACCESS_DENIED;
                case 404 -> FailureKind.MODEL_UNAVAILABLE;
                case 429 -> isQuotaExceeded(serviceException)
                        ? FailureKind.QUOTA_EXCEEDED
                        : FailureKind.RATE_LIMITED;
                default -> FailureKind.UPSTREAM;
            };
        }
        return FailureKind.UPSTREAM;
    }

    private static boolean isQuotaExceeded(OpenAIServiceException exception) {
        return exception.code().filter(QUOTA_ERROR_CODES::contains).isPresent()
                || exception.type().filter("insufficient_quota"::equals).isPresent();
    }

    public static String serialize(ObjectMapper objectMapper, Object input) {
        try {
            return objectMapper.writeValueAsString(input);
        } catch (JsonProcessingException exception) {
            throw new InvalidOutputException(exception);
        }
    }

    public enum FailureKind {
        AUTHENTICATION,
        ACCESS_DENIED,
        MODEL_UNAVAILABLE,
        QUOTA_EXCEEDED,
        RATE_LIMITED,
        REQUEST_REJECTED,
        TIMEOUT,
        UPSTREAM,
        INVALID_OUTPUT
    }

    public static final class InvalidOutputException extends RuntimeException {

        public InvalidOutputException() {
        }

        public InvalidOutputException(Throwable cause) {
            super(cause);
        }
    }

    public static final class UpstreamResponseException extends RuntimeException {
    }
}
