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

    private static final String INSTRUCTIONS = "입력된 점수는 데이비스 점수이며 높을수록 튼살이 많음을 의미합니다. "
            + "날짜별 점수 변화만 내부 근거로 사용해 사용자의 변화와 고민을 추론하여 명확하되 공감되며 마음을 안정시키는 한국어 멘트를 작성하세요. "
            + "결과에는 점수, 수치, 데이비스라는 표현을 노출하지 말고 제공되지 않은 증상이나 사실을 추측하지 마세요. "
            + "의료적 진단이나 치료 효과를 단정하지 말고 이모지 없이 공백과 문장부호를 포함해 60자 이상 70자 이하로 작성하세요.";

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
                            .maxOutputTokens(5000)
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
        return new ReportPointInput(point.checkInDate(), point.score());
    }

    private static GenerationException.Reason reasonFor(
            OpenAiStructuredResponseSupport.FailureKind failureKind) {
        return switch (failureKind) {
            case TIMEOUT -> GenerationException.Reason.TIMEOUT;
            case AUTHENTICATION, ACCESS_DENIED, MODEL_UNAVAILABLE, QUOTA_EXCEEDED,
                    RATE_LIMITED, REQUEST_REJECTED, UPSTREAM -> GenerationException.Reason.UPSTREAM;
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

    private record ReportPointInput(LocalDate checkInDate, short score) {
    }
}
