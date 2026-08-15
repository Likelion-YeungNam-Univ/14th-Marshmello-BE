package Marshmello.MarshmelloWas.domain.checkin.adapter;

import Marshmello.MarshmelloWas.domain.checkin.port.ImageAnalyzer;
import org.springframework.context.annotation.Fallback;
import org.springframework.stereotype.Component;

@Component
@Fallback
public class UnavailableImageAnalyzer implements ImageAnalyzer {

    @Override
    public AnalysisResult analyze(byte[] image) {
        throw new ImageAnalysisException(ImageAnalysisException.Reason.UNAVAILABLE);
    }
}
