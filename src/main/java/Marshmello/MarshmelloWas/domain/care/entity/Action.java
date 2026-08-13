package Marshmello.MarshmelloWas.domain.care.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

    @Column(name = "source", nullable = false, length = 50)
    private String source;

    @Column(name = "helpfulness_score")
    private Short helpfulnessScore;

    protected Action() {
    }

    public Action(
            short actionScore,
            String category,
            String guideText,
            String source,
            Short helpfulnessScore
    ) {
        this.actionScore = actionScore;
        this.category = category;
        this.guideText = guideText;
        this.source = source;
        this.helpfulnessScore = helpfulnessScore;
    }

    public Long getActionId() {
        return actionId;
    }

    public short getActionScore() {
        return actionScore;
    }

    public String getCategory() {
        return category;
    }

    public String getGuideText() {
        return guideText;
    }

    public String getSource() {
        return source;
    }

    public Short getHelpfulnessScore() {
        return helpfulnessScore;
    }
}
