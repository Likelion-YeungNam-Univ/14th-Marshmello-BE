package Marshmello.MarshmelloWas.domain.report.adapter;

import Marshmello.MarshmelloWas.domain.report.dto.ReportGenerationRequest;
import Marshmello.MarshmelloWas.domain.report.port.ReportGenerator;
import Marshmello.MarshmelloWas.domain.report.port.ReportGenerator.GeneratedContent;
import Marshmello.MarshmelloWas.domain.report.port.ReportGenerator.GenerationException;
import org.springframework.context.annotation.Fallback;
import org.springframework.stereotype.Component;

@Component
@Fallback
public class UnavailableReportGenerator implements ReportGenerator {

    @Override
    public GeneratedContent generate(ReportGenerationRequest request) {
        throw new GenerationException(GenerationException.Reason.UNAVAILABLE);
    }
}
