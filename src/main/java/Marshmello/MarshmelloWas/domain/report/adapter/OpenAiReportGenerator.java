package Marshmello.MarshmelloWas.domain.report.adapter;

import Marshmello.MarshmelloWas.domain.report.dto.ReportGenerationRequest;
import Marshmello.MarshmelloWas.domain.report.port.ReportGenerator;
import Marshmello.MarshmelloWas.domain.report.port.ReportGenerator.GeneratedContent;
import Marshmello.MarshmelloWas.domain.report.port.ReportGenerator.GenerationException;
import Marshmello.MarshmelloWas.infrastructure.ai.openai.OpenAiStructuredResponseSupport;
import Marshmello.MarshmelloWas.domain.report.dto.ReportTrendPoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.errors.OpenAIException;
import com.openai.models.responses.StructuredResponse;
import com.openai.models.responses.StructuredResponseCreateParams;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public final class OpenAiReportGenerator implements ReportGenerator {

    private static final String INSTRUCTIONS = "기간별 점수와 달성 여부의 흐름만 근거로 한국어 리포트를 작성하세요. "
            + "관찰된 변화, 긍정적인 점, 부담 없는 다음 행동을 명확히 구분하고 제공되지 않은 사실은 추측하지 마세요.";

    private final OpenAIClient client;
    private final String model;
    private final ObjectMapper objectMapper;

    public OpenAiReportGenerator(OpenAIClient client, String model, ObjectMapper objectMapper) {
        this.client = Objects.requireNonNull(client, "client");
        this.model = Objects.requireNonNull(model, "model");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public GeneratedContent generate(ReportGenerationRequest request) {
        try {
            List<ReportPointInput> points = request.trendPoints().stream()
                    .map(OpenAiReportGenerator::toInput)
                    .toList();
            StructuredResponseCreateParams<ReportStructuredOutput> params =
                    StructuredResponseCreateParams.<ReportStructuredOutput>builder()
                            .model(model)
                            .instructions(INSTRUCTIONS)
                            .input(OpenAiStructuredResponseSupport.serialize(objectMapper, new ReportInput(points)))
                            .text(ReportStructuredOutput.class)
                            .store(false)
                            .maxOutputTokens(2500)
                            .build();
            StructuredResponse<ReportStructuredOutput> response = client.responses().create(params);
            ReportStructuredOutput output =
                    OpenAiStructuredResponseSupport.requireSingleCompletedOutput(response);
            try {
                return new GeneratedContent(output.content);
            } catch (IllegalArgumentException | NullPointerException exception) {
                throw new OpenAiStructuredResponseSupport.InvalidOutputException(exception);
            }
        } catch (OpenAIException exception) {
            throw generationFailure(reasonFor(OpenAiStructuredResponseSupport.classify(exception)), exception);
        } catch (OpenAiStructuredResponseSupport.InvalidOutputException exception) {
            throw generationFailure(GenerationException.Reason.INVALID_OUTPUT, exception);
        } catch (OpenAiStructuredResponseSupport.UpstreamResponseException exception) {
            throw generationFailure(GenerationException.Reason.UPSTREAM, exception);
        }
    }

    private static ReportPointInput toInput(ReportTrendPoint point) {
        return new ReportPointInput(point.checkInDate(), point.score(), point.achieved());
    }

    private static GenerationException.Reason reasonFor(
            OpenAiStructuredResponseSupport.FailureKind failureKind) {
        return switch (failureKind) {
            case TIMEOUT -> GenerationException.Reason.TIMEOUT;
            case UPSTREAM -> GenerationException.Reason.UPSTREAM;
            case INVALID_OUTPUT -> GenerationException.Reason.INVALID_OUTPUT;
        };
    }

    private static GenerationException generationFailure(
            GenerationException.Reason reason,
            Throwable cause) {
        return new GenerationException(reason, cause);
    }

    private record ReportInput(List<ReportPointInput> points) {
    }

    private record ReportPointInput(LocalDate checkInDate, short score, boolean achieved) {
    }
}
