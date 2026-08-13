package Marshmello.MarshmelloWas.infrastructure.ai.openai;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import Marshmello.MarshmelloWas.domain.care.adapter.OpenAiCareCardGenerator;
import Marshmello.MarshmelloWas.domain.report.adapter.OpenAiReportGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
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
}
