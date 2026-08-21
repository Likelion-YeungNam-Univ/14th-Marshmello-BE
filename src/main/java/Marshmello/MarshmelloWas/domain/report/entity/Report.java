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
import java.time.YearMonth;
import java.util.Objects;
import org.hibernate.annotations.Check;

@Entity
@Table(
        name = "report",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_report_user_month",
                columnNames = {"user_id", "report_month"}
        ),
        indexes = @Index(name = "idx_report_user_month", columnList = "user_id, report_month DESC")
)
@Check(name = "ck_report_month_first_day", constraints = "EXTRACT(DAY FROM report_month) = 1")
public class Report {

    private static final int MIN_CONTENT_LENGTH = 60;
    private static final int MAX_CONTENT_LENGTH = 70;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "report_month", nullable = false)
    private LocalDate reportMonth;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    protected Report() {
    }

    public Report(String content, YearMonth reportMonth, Long userId) {
        this.content = validatedContent(content);
        this.reportMonth = Objects.requireNonNull(reportMonth).atDay(1);
        this.userId = Objects.requireNonNull(userId);
    }

    private static String validatedContent(String content) {
        String trimmed = Objects.requireNonNull(content).trim();
        if (trimmed.length() < MIN_CONTENT_LENGTH || trimmed.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("content length must be between 60 and 70 characters");
        }
        return trimmed;
    }

    public Long getReportId() {
        return reportId;
    }

    public String getContent() {
        return content;
    }

    public YearMonth getReportMonth() {
        return YearMonth.from(reportMonth);
    }

    public Long getUserId() {
        return userId;
    }
}
