package Marshmello.MarshmelloWas.domain.checkin.adapter;

import Marshmello.MarshmelloWas.domain.checkin.port.ImageAnalyzer;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
@ConditionalOnProperty(
        prefix = "app.analysis.onnx",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class OnnxImageAnalyzerConfiguration {

    @Bean
    OrtEnvironment ortEnvironment() {
        return OrtEnvironment.getEnvironment();
    }

    @Bean(name = "anatomyLocatorSession", destroyMethod = "close")
    OrtSession anatomyLocatorSession(OrtEnvironment environment) throws IOException, OrtException {
        return createSession(environment, "models/best_anatomy_locator.onnx");
    }

    @Bean(name = "stretchUnetSession", destroyMethod = "close")
    OrtSession stretchUnetSession(OrtEnvironment environment) throws IOException, OrtException {
        return createSession(environment, "models/best_unet.onnx");
    }

    @Bean
    DaveyOnnxModelRunner daveyOnnxModelRunner(
            OrtEnvironment environment,
            @Qualifier("anatomyLocatorSession") OrtSession anatomyLocatorSession,
            @Qualifier("stretchUnetSession") OrtSession stretchUnetSession
    ) throws OrtException {
        return new DaveyOnnxModelRunner(environment, anatomyLocatorSession, stretchUnetSession);
    }

    @Bean
    ImageAnalyzer imageAnalyzer(DaveyOnnxModelRunner modelRunner) {
        return new OnnxImageAnalyzer(
                modelRunner,
                new OnnxImagePreprocessor(modelRunner.imageSize()),
                new DaveyPostProcessor());
    }

    private OrtSession createSession(OrtEnvironment environment, String modelPath)
            throws IOException, OrtException {
        ClassPathResource resource = new ClassPathResource(modelPath);
        byte[] model;
        try (InputStream input = resource.getInputStream()) {
            model = input.readAllBytes();
        }
        try (OrtSession.SessionOptions options = new OrtSession.SessionOptions()) {
            return environment.createSession(model, options);
        }
    }
}
