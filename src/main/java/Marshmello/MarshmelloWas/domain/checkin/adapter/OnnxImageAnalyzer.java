package Marshmello.MarshmelloWas.domain.checkin.adapter;

import Marshmello.MarshmelloWas.domain.checkin.adapter.OnnxImagePreprocessor.PreparedImage;
import Marshmello.MarshmelloWas.domain.checkin.port.ImageAnalyzer;
import ai.onnxruntime.OrtException;
import java.io.IOException;

public final class OnnxImageAnalyzer implements ImageAnalyzer {

    private final DaveyOnnxModelRunner modelRunner;
    private final OnnxImagePreprocessor preprocessor;
    private final DaveyPostProcessor postProcessor;

    OnnxImageAnalyzer(
            DaveyOnnxModelRunner modelRunner,
            OnnxImagePreprocessor preprocessor,
            DaveyPostProcessor postProcessor
    ) {
        this.modelRunner = modelRunner;
        this.preprocessor = preprocessor;
        this.postProcessor = postProcessor;
    }

    @Override
    public AnalysisResult analyze(byte[] image) {
        try {
            PreparedImage prepared = preprocessor.prepare(image);
            return postProcessor.evaluate(prepared.image(), modelRunner.predict(prepared));
        } catch (IOException exception) {
            throw new ImageAnalysisException(ImageAnalysisException.Reason.INVALID_IMAGE, exception);
        } catch (OrtException | IllegalArgumentException | IllegalStateException exception) {
            throw new ImageAnalysisException(ImageAnalysisException.Reason.FAILED, exception);
        }
    }
}
