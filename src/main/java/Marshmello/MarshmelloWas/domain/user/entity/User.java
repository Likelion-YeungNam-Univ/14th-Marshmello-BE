package Marshmello.MarshmelloWas.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;

@Getter
@Entity
@Table(name = "users")
public class User {

    private static final String DEFAULT_NICKNAME = "마시멜로";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "nickname", nullable = false, length = 15)
    private String nickname;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(name = "profile_completed", nullable = false)
    private boolean profileCompleted;

    protected User() {
    }

    public User(String nickname, LocalDate expectedDeliveryDate) {
        this.nickname = nickname;
        this.expectedDeliveryDate = expectedDeliveryDate;
        this.profileCompleted = true;
    }

    public static User createPendingProfile() {
        User user = new User();
        user.nickname = DEFAULT_NICKNAME;
        user.profileCompleted = false;
        return user;
    }

    public void updateProfile(String nickname, LocalDate expectedDeliveryDate) {
        this.nickname = nickname;
        this.expectedDeliveryDate = expectedDeliveryDate;
        this.profileCompleted = true;
    }
}
