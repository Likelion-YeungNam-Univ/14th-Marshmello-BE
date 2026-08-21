package Marshmello.MarshmelloWas.domain.care.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.Getter;

@Getter
@Entity
@Table(
        name = "care_card",
        uniqueConstraints = @UniqueConstraint(name = "uq_care_card_checkin", columnNames = "checkin_id"),
        indexes = @Index(name = "idx_care_card_action_id", columnList = "action_id")
)
public class CareCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "care_card_id", nullable = false)
    private Long careCardId;

    @Column(name = "action_name", nullable = false, length = 50)
    private String actionName;

    @Column(name = "action_reason", nullable = false, columnDefinition = "TEXT")
    private String actionReason;

    @Column(name = "source", nullable = false, length = 100)
    private String source;

    @Column(name = "checkin_id", nullable = false, unique = true)
    private Long checkInId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "action_id", nullable = false)
    private Action action;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDate createdDate;

    protected CareCard() {
    }

    public CareCard(
            String actionName,
            String actionReason,
            String source,
            Long checkInId,
            Action action,
            LocalDate createdDate
    ) {
        this.actionName = actionName;
        this.actionReason = actionReason;
        this.source = source;
        this.checkInId = checkInId;
        this.action = action;
        this.createdDate = createdDate;
    }

}
