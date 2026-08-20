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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.errors.OpenAIIoException;
import com.openai.models.ReasoningEffort;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.openai.services.blocking.ResponseService;
import java.io.IOException;
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
        assertThat(params.rawParams().maxOutputTokens()).contains(1200L);
    }
}
