package Marshmello.MarshmelloWas.infrastructure.ai.internal;

import Marshmello.MarshmelloWas.domain.analysis.model.ImageAnalysisException;
import Marshmello.MarshmelloWas.domain.analysis.port.ImageAnalyzer;
import org.springframework.context.annotation.Fallback;
import org.springframework.stereotype.Component;

@Component
@Fallback
public class UnavailableImageAnalyzer implements ImageAnalyzer {

    @Override
    public short analyze(byte[] imageData) {
        throw new ImageAnalysisException(ImageAnalysisException.Reason.UNAVAILABLE);
    }
}
