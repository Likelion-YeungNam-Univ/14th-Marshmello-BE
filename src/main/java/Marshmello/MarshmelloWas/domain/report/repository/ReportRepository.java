package Marshmello.MarshmelloWas.domain.report.repository;

import Marshmello.MarshmelloWas.domain.report.entity.Report;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    Optional<Report> findByUserIdAndReportMonth(Long userId, LocalDate reportMonth);
}
