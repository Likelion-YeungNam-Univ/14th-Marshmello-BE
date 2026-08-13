package Marshmello.MarshmelloWas.domain.report.port;

import Marshmello.MarshmelloWas.domain.report.dto.ReportTrendPoint;
import Marshmello.MarshmelloWas.domain.report.dto.ReportView;
import Marshmello.MarshmelloWas.domain.report.entity.Report;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReportSourcePort {

    Optional<ReportView> findExistingByUserIdAndPeriod(long userId, LocalDate periodStart, LocalDate periodEnd);

    List<ReportTrendPoint> findTrendPointsByUserIdAndCheckInDateBetweenInclusive(
            long userId,
            LocalDate periodStart,
            LocalDate periodEnd
    );
}
