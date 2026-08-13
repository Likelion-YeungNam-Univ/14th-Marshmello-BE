package Marshmello.MarshmelloWas.domain.care.adapter;

import Marshmello.MarshmelloWas.domain.care.dto.CareCardGenerationRequest;
import Marshmello.MarshmelloWas.domain.care.port.CareCardGenerator;
import Marshmello.MarshmelloWas.domain.care.port.CareCardGenerator.CareCardGeneratedText;
import Marshmello.MarshmelloWas.domain.care.port.CareCardGenerator.CareCardGenerationException;
import Marshmello.MarshmelloWas.infrastructure.ai.openai.OpenAiStructuredResponseSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.errors.OpenAIException;
import com.openai.models.responses.StructuredResponse;
import com.openai.models.responses.StructuredResponseCreateParams;

import java.util.Objects;

public final class OpenAiCareCardGenerator implements CareCardGenerator {

    private static final String INSTRUCTIONS = "사용자가 실천할 수 있는 짧고 구체적인 돌봄 행동을 한국어로 작성하세요. "
            + "actionName은 50자 이하의 행동 제목이고 actionReason은 그 행동이 도움이 되는 이유여야 합니다.";

    private final OpenAIClient client;
    private final String model;
    private final ObjectMapper objectMapper;

    public OpenAiCareCardGenerator(OpenAIClient client, String model, ObjectMapper objectMapper) {
        this.client = Objects.requireNonNull(client, "client");
        this.model = Objects.requireNonNull(model, "model");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public CareCardGeneratedText generate(CareCardGenerationRequest request) {
        try {
            StructuredResponseCreateParams<CareCardStructuredOutput> params =
                    StructuredResponseCreateParams.<CareCardStructuredOutput>builder()
                            .model(model)
                            .instructions(INSTRUCTIONS)
                            .input(OpenAiStructuredResponseSupport.serialize(
                                    objectMapper,
                                    new CareCardInput(request.category(), request.guideText())
                            ))
                            .text(CareCardStructuredOutput.class)
                            .store(false)
                            .maxOutputTokens(400)
                            .build();
            StructuredResponse<CareCardStructuredOutput> response = client.responses().create(params);
            CareCardStructuredOutput output =
                    OpenAiStructuredResponseSupport.requireSingleCompletedOutput(response);
            try {
                return new CareCardGeneratedText(output.actionName, output.actionReason);
            } catch (IllegalArgumentException | NullPointerException exception) {
                throw new OpenAiStructuredResponseSupport.InvalidOutputException(exception);
            }
        } catch (OpenAIException exception) {
            throw generationFailure(reasonFor(OpenAiStructuredResponseSupport.classify(exception)), exception);
        } catch (OpenAiStructuredResponseSupport.InvalidOutputException exception) {
            throw generationFailure(CareCardGenerationException.Reason.INVALID_OUTPUT, exception);
        } catch (OpenAiStructuredResponseSupport.UpstreamResponseException exception) {
            throw generationFailure(CareCardGenerationException.Reason.UPSTREAM, exception);
        }
    }

    private static CareCardGenerationException.Reason reasonFor(
            OpenAiStructuredResponseSupport.FailureKind failureKind) {
        return switch (failureKind) {
            case TIMEOUT -> CareCardGenerationException.Reason.TIMEOUT;
            case UPSTREAM -> CareCardGenerationException.Reason.UPSTREAM;
            case INVALID_OUTPUT -> CareCardGenerationException.Reason.INVALID_OUTPUT;
        };
    }

    private static CareCardGenerationException generationFailure(
            CareCardGenerationException.Reason reason,
            Throwable cause) {
        return new CareCardGenerationException(reason, cause);
    }

    private record CareCardInput(String category, String guideText) {
    }
}
