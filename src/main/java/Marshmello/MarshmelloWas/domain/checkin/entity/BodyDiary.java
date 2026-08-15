package Marshmello.MarshmelloWas.domain.checkin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "body_diary")
public class BodyDiary {

    @EmbeddedId
    private BodyDiaryId bodyDiaryId;

    @MapsId("checkInId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "checkin_id", nullable = false)
    private CheckIn checkIn;

    @Column(name = "stretch_mark")
    private Boolean stretchMark;

    @Column(name = "comment", length = 50)
    private String comment;

    protected BodyDiary() {
    }

    public BodyDiary(BodyDiaryId bodyDiaryId, CheckIn checkIn, Boolean stretchMark, String comment) {
        this.bodyDiaryId = bodyDiaryId;
        this.checkIn = checkIn;
        this.stretchMark = stretchMark;
        this.comment = comment;
    }

    public short bodyRegion() {
        return bodyDiaryId.bodyRegion();
    }

    public BodyDiaryId id() {
        return bodyDiaryId;
    }

    public Boolean stretchMark() {
        return stretchMark;
    }

    public String comment() {
        return comment;
    }
}
