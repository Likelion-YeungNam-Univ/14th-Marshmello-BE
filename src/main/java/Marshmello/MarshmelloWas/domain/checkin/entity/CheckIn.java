package Marshmello.MarshmelloWas.domain.checkin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "check_in")
public class CheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "checkin_id", nullable = false)
    private Long checkInId;

    @Column(name = "achieved", nullable = false)
    private boolean achieved;

    @Column(name = "checkin_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "diary", length = 255)
    private String diary;

    @Column(name = "emotion", nullable = false)
    private short emotion;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @OneToOne(mappedBy = "checkIn", fetch = jakarta.persistence.FetchType.LAZY)
    private Image image;

    protected CheckIn() {
    }

    public CheckIn(boolean achieved, LocalDate checkInDate, String diary, short emotion, Long userId) {
        this.achieved = achieved;
        this.checkInDate = checkInDate;
        this.diary = diary;
        this.emotion = emotion;
        this.userId = userId;
    }

    public Long id() {
        return checkInId;
    }

    public boolean achieved() {
        return achieved;
    }

    public LocalDate date() {
        return checkInDate;
    }

    public String diary() {
        return diary;
    }

    public short emotion() {
        return emotion;
    }

    public Long userId() {
        return userId;
    }

    public Long imageId() {
        return image == null ? null : image.id();
    }

    void attachImage(Image image) {
        if (this.image != null && this.image != image) {
            throw new IllegalStateException("Check-in already has an image");
        }
        this.image = image;
    }
}
