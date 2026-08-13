package Marshmello.MarshmelloWas.domain.report.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import org.hibernate.annotations.Check;

@Entity
@Table(
        name = "report",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_report_user_period",
                columnNames = {"user_id", "period_start", "period_end"}
        ),
        indexes = @Index(name = "idx_report_user_period_start", columnList = "user_id, period_start DESC")
)
@Check(name = "ck_report_period", constraints = "period_start <= period_end")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    protected Report() {
    }

    public Report(String content, LocalDate periodStart, LocalDate periodEnd, Long userId) {
        this.content = content;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.userId = userId;
    }

    public Long getReportId() {
        return reportId;
    }

    public String getContent() {
        return content;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public Long getUserId() {
        return userId;
    }
}
