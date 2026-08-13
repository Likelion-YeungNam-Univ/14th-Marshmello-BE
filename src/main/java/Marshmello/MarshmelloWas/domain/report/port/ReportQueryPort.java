package Marshmello.MarshmelloWas.domain.report.port;

import Marshmello.MarshmelloWas.domain.report.dto.ReportPageRequest;
import Marshmello.MarshmelloWas.domain.report.dto.ReportSummary;
import Marshmello.MarshmelloWas.domain.report.dto.ReportView;
import Marshmello.MarshmelloWas.domain.report.entity.Report;
import Marshmello.MarshmelloWas.domain.report.model.ReportPage;

import java.util.Optional;

public interface ReportQueryPort {

    Optional<ReportView> findOwnedById(long reportId, long userId);

    ReportPage<ReportSummary> findPageByUserId(long userId, ReportPageRequest pageRequest);
}
