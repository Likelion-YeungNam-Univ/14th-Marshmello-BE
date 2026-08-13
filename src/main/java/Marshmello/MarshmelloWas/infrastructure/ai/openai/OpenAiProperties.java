package Marshmello.MarshmelloWas.infrastructure.ai.openai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("openai")
public class OpenAiProperties {

    static final String DEFAULT_MODEL = "gpt-5.6-luna";

    private String apiKey = "";
    private String model = DEFAULT_MODEL;
    private Duration timeout = Duration.ofSeconds(30);

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    String requiredApiKey() {
        return apiKey.trim();
    }

    String effectiveModel() {
        return model == null || model.isBlank() ? DEFAULT_MODEL : model.trim();
    }

    Duration effectiveTimeout() {
        return timeout == null ? Duration.ofSeconds(30) : timeout;
    }
}
