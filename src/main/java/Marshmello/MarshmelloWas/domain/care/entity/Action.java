package Marshmello.MarshmelloWas.domain.care.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "actions")
public class Action {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "action_id", nullable = false)
    private Long actionId;

    @Column(name = "action_score", nullable = false)
    private short actionScore;

    @Column(name = "category", nullable = false, length = 20)
    private String category;

    @Column(name = "guide_text", nullable = false, columnDefinition = "TEXT")
    private String guideText;

    @Column(name = "source", nullable = false, length = 100)
    private String source;

    protected Action() {
    }

    public Action(
            short actionScore,
            String category,
            String guideText,
            String source
    ) {
        if (actionScore < 0 || actionScore > 8) {
            throw new IllegalArgumentException("actionScore must be between 0 and 8");
        }
        this.actionScore = actionScore;
        this.category = category;
        this.guideText = guideText;
        this.source = source;
    }

}
