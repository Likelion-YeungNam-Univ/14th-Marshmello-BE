package Marshmello.MarshmelloWas.domain.checkin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class BodyDiaryId implements Serializable {

    @Column(name = "body_region", nullable = false)
    private short bodyRegion;

    @Column(name = "checkin_id", nullable = false)
    private Long checkInId;

    protected BodyDiaryId() {
    }

    public BodyDiaryId(short bodyRegion, Long checkInId) {
        this.bodyRegion = bodyRegion;
        this.checkInId = checkInId;
    }

    public short bodyRegion() {
        return bodyRegion;
    }

    public Long checkInId() {
        return checkInId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BodyDiaryId that)) {
            return false;
        }
        return bodyRegion == that.bodyRegion && Objects.equals(checkInId, that.checkInId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bodyRegion, checkInId);
    }
}
