package Marshmello.MarshmelloWas.infrastructure.ai.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.errors.OpenAIIoException;
import com.openai.errors.OpenAIException;
import com.openai.errors.OpenAIInvalidDataException;
import com.openai.models.responses.ResponseOutputMessage;
import com.openai.models.responses.ResponseStatus;
import com.openai.models.responses.StructuredResponse;
import com.openai.models.responses.StructuredResponseOutputItem;
import com.openai.models.responses.StructuredResponseOutputMessage;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

final class OpenAiStructuredResponseSupport {

    private OpenAiStructuredResponseSupport() {
    }

    static <T> T requireSingleCompletedOutput(StructuredResponse<T> response) {
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

    static FailureKind classify(OpenAIException exception) {
        if (exception instanceof OpenAIIoException ioException && isTimeout(ioException)) {
            return FailureKind.TIMEOUT;
        }
        if (exception instanceof OpenAIInvalidDataException) {
            return FailureKind.INVALID_OUTPUT;
        }
        return FailureKind.UPSTREAM;
    }

    static String serialize(ObjectMapper objectMapper, Object input) {
        try {
            return objectMapper.writeValueAsString(input);
        } catch (JsonProcessingException exception) {
            throw new InvalidOutputException(exception);
        }
    }

    enum FailureKind {
        TIMEOUT,
        UPSTREAM,
        INVALID_OUTPUT
    }

    static final class InvalidOutputException extends RuntimeException {

        InvalidOutputException() {
        }

        InvalidOutputException(Throwable cause) {
            super(cause);
        }
    }

    static final class UpstreamResponseException extends RuntimeException {
    }
}
