package Marshmello.MarshmelloWas.infrastructure.ai.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@Conditional(OpenAiEnabledCondition.class)
@EnableConfigurationProperties(OpenAiProperties.class)
public class OpenAiConfiguration {

    @Bean(destroyMethod = "close")
    OpenAIClient openAIClient(OpenAiProperties properties) {
        return OpenAIOkHttpClient.builder()
                .apiKey(properties.requiredApiKey())
                .timeout(properties.effectiveTimeout())
                .maxRetries(0)
                .build();
    }

    @Bean
    OpenAiCareCardGenerator openAiCareCardGenerator(
            OpenAIClient client,
            OpenAiProperties properties,
            ObjectMapper objectMapper
    ) {
        return new OpenAiCareCardGenerator(client, properties.effectiveModel(), objectMapper);
    }

    @Bean
    OpenAiReportGenerator openAiReportGenerator(
            OpenAIClient client,
            OpenAiProperties properties,
            ObjectMapper objectMapper
    ) {
        return new OpenAiReportGenerator(client, properties.effectiveModel(), objectMapper);
    }
}
