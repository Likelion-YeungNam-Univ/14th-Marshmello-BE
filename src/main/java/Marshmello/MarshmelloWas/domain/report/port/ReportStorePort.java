package Marshmello.MarshmelloWas.domain.report.port;

import Marshmello.MarshmelloWas.domain.report.dto.CreateReportCommand;
import Marshmello.MarshmelloWas.domain.report.dto.ReportCreationResult;
import Marshmello.MarshmelloWas.domain.report.entity.Report;
import Marshmello.MarshmelloWas.domain.report.model.ReportGeneratedContent;

public interface ReportStorePort {

    ReportCreationResult save(CreateReportCommand command, ReportGeneratedContent generatedContent);
}
