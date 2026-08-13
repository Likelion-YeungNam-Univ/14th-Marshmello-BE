package Marshmello.MarshmelloWas.infrastructure.ai.openai;

import Marshmello.MarshmelloWas.domain.report.model.ReportGeneratedContent;
import Marshmello.MarshmelloWas.domain.report.model.ReportGenerationException;
import Marshmello.MarshmelloWas.domain.report.dto.ReportGenerationRequest;
import Marshmello.MarshmelloWas.domain.report.service.port.ReportGenerator;
import org.springframework.context.annotation.Fallback;
import org.springframework.stereotype.Component;

@Component
@Fallback
public class UnavailableReportGenerator implements ReportGenerator {

    @Override
    public ReportGeneratedContent generate(ReportGenerationRequest request) {
        throw new ReportGenerationException(ReportGenerationException.Reason.UNAVAILABLE);
    }
}
