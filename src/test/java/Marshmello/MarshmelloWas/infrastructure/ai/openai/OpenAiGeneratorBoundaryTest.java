package Marshmello.MarshmelloWas.infrastructure.ai.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import Marshmello.MarshmelloWas.domain.care.adapter.OpenAiCareCardGenerator;
import Marshmello.MarshmelloWas.domain.care.adapter.CareCardStructuredOutput;
import Marshmello.MarshmelloWas.domain.care.dto.CareCardGenerationRequest;
import Marshmello.MarshmelloWas.domain.care.port.CareCardGenerator.CareCardGenerationException;
import Marshmello.MarshmelloWas.domain.report.adapter.OpenAiReportGenerator;
import Marshmello.MarshmelloWas.domain.report.adapter.ReportStructuredOutput;
import Marshmello.MarshmelloWas.domain.report.dto.ReportGenerationRequest;
import Marshmello.MarshmelloWas.domain.report.dto.ReportTrendPoint;
import Marshmello.MarshmelloWas.domain.report.port.ReportGenerator.GenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.errors.OpenAIIoException;
import com.openai.models.ReasoningEffort;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.openai.services.blocking.ResponseService;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.junit.jupiter.api.Test;

class OpenAiGeneratorBoundaryTest {

    @Test
    void convertsSerializationFailuresToInvalidOutput() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        when(objectMapper.writeValueAsString(any()))
                .thenThrow(mock(JsonProcessingException.class));

        assertThatThrownBy(() -> OpenAiStructuredResponseSupport.serialize(objectMapper, new Object()))
                .isInstanceOf(OpenAiStructuredResponseSupport.InvalidOutputException.class)
                .hasCauseInstanceOf(JsonProcessingException.class);
    }

    @Test
    void doesNotMaskCareCardProgrammingErrorsAsProviderFailures() {
        OpenAiCareCardGenerator generator = new OpenAiCareCardGenerator(
                mock(OpenAIClient.class),
                "test-model",
                mock(ObjectMapper.class)
        );

        assertThatThrownBy(() -> generator.generate(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void doesNotMaskReportProgrammingErrorsAsProviderFailures() {
        OpenAiReportGenerator generator = new OpenAiReportGenerator(
                mock(OpenAIClient.class),
                "test-model",
                mock(ObjectMapper.class)
        );

        assertThatThrownBy(() -> generator.generate(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void requestsCareCardWithLowReasoningAndSufficientOutputBudget() {
        OpenAIClient client = mock(OpenAIClient.class);
        ResponseService responses = mock(ResponseService.class);
        when(client.responses()).thenReturn(responses);
        when(responses.create(ArgumentMatchers
                .<StructuredResponseCreateParams<CareCardStructuredOutput>>any()))
                .thenThrow(new OpenAIIoException("test failure", new IOException("test failure")));
        OpenAiCareCardGenerator generator = new OpenAiCareCardGenerator(
                client,
                "gpt-5.6-luna",
                new ObjectMapper()
        );

        assertThatThrownBy(() -> generator.generate(new CareCardGenerationRequest("category", "guide")))
                .isInstanceOf(CareCardGenerationException.class);

        ArgumentCaptor<StructuredResponseCreateParams<CareCardStructuredOutput>> captor =
                ArgumentCaptor.captor();
        verify(responses).create(captor.capture());
        StructuredResponseCreateParams<CareCardStructuredOutput> params = captor.getValue();

        assertThat(params.rawParams().reasoning().orElseThrow().effort())
                .contains(ReasoningEffort.LOW);
        assertThat(params.rawParams().maxOutputTokens()).contains(5000L);
    }

    @Test
    void requestsReportWithSufficientOutputBudget() {
        OpenAIClient client = mock(OpenAIClient.class);
        ResponseService responses = mock(ResponseService.class);
        when(client.responses()).thenReturn(responses);
        when(responses.create(ArgumentMatchers
                .<StructuredResponseCreateParams<ReportStructuredOutput>>any()))
                .thenThrow(new OpenAIIoException("test failure", new IOException("test failure")));
        OpenAiReportGenerator generator = new OpenAiReportGenerator(
                client,
                "gpt-5.6-luna",
                new ObjectMapper().findAndRegisterModules()
        );
        ReportGenerationRequest request = new ReportGenerationRequest(List.of(
                new ReportTrendPoint(LocalDate.of(2026, 8, 1), (short) 1),
                new ReportTrendPoint(LocalDate.of(2026, 8, 2), (short) 2)
        ));

        assertThatThrownBy(() -> generator.generate(request))
                .isInstanceOf(GenerationException.class);

        ArgumentCaptor<StructuredResponseCreateParams<ReportStructuredOutput>> captor =
                ArgumentCaptor.captor();
        verify(responses).create(captor.capture());

        assertThat(captor.getValue().rawParams().maxOutputTokens()).contains(5000L);
    }
}
