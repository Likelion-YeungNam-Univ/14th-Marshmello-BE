package Marshmello.MarshmelloWas.domain.checkin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "images")
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id", nullable = false)
    private Long imageId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "object_key", length = 1024)
    private String objectKey;

    @Column(name = "content_type", length = 255)
    private String contentType;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkin_id", unique = true)
    private CheckIn checkIn;

    protected Image() {
    }

    public Image(long userId, String objectKey, String contentType, Instant createdAt) {
        this.userId = userId;
        this.objectKey = Objects.requireNonNull(objectKey);
        this.contentType = contentType;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public Long id() {
        return imageId;
    }

    public String objectKey() {
        return objectKey;
    }

    public String contentType() {
        return contentType;
    }

    public boolean belongsTo(long userId) {
        return this.userId == userId;
    }

    public boolean isAttached() {
        return checkIn != null;
    }

    public Long checkInId() {
        return checkIn == null ? null : checkIn.id();
    }

    public void attachTo(CheckIn checkIn, long userId) {
        if (!belongsTo(userId)) {
            throw new IllegalArgumentException("Image does not belong to the user");
        }
        if (this.checkIn != null) {
            throw new IllegalStateException("Image is already attached");
        }
        this.checkIn = Objects.requireNonNull(checkIn);
        checkIn.attachImage(this);
    }
}
