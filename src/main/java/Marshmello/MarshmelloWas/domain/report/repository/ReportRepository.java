package Marshmello.MarshmelloWas.domain.report.repository;

import Marshmello.MarshmelloWas.domain.report.entity.Report;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    Optional<Report> findByReportIdAndUserId(Long reportId, Long userId);

    Optional<Report> findByUserIdAndPeriodStartAndPeriodEnd(Long userId, LocalDate periodStart, LocalDate periodEnd);

    Page<Report> findByUserIdOrderByPeriodStartDescReportIdDesc(Long userId, Pageable pageable);
}
