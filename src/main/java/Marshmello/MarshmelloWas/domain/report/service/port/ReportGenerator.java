package Marshmello.MarshmelloWas.domain.report.service.port;

import Marshmello.MarshmelloWas.domain.report.dto.ReportGenerationRequest;
import Marshmello.MarshmelloWas.domain.report.entity.Report;
import Marshmello.MarshmelloWas.domain.report.model.ReportGeneratedContent;

public interface ReportGenerator {

    ReportGeneratedContent generate(ReportGenerationRequest request);
}
