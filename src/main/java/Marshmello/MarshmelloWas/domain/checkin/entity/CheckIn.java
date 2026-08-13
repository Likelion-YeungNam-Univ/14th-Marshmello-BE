package Marshmello.MarshmelloWas.domain.checkin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

    protected CheckIn() {
    }

    public CheckIn(boolean achieved, LocalDate checkInDate, String diary, short emotion, Long userId) {
        this.achieved = achieved;
        this.checkInDate = checkInDate;
        this.diary = diary;
        this.emotion = emotion;
        this.userId = userId;
    }

    public Long getCheckInId() {
        return checkInId;
    }

    public boolean isAchieved() {
        return achieved;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public String getDiary() {
        return diary;
    }

    public short getEmotion() {
        return emotion;
    }

    public Long getUserId() {
        return userId;
    }
}
