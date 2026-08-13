package Marshmello.MarshmelloWas.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    protected User() {
    }

    public User(String nickname, LocalDate expectedDeliveryDate) {
        this.nickname = nickname;
        this.expectedDeliveryDate = expectedDeliveryDate;
    }

    public void updateProfile(String nickname, LocalDate expectedDeliveryDate) {
        this.nickname = nickname;
        this.expectedDeliveryDate = expectedDeliveryDate;
    }

    public Long getUserId() {
        return userId;
    }

    public String getNickname() {
        return nickname;
    }

    public LocalDate getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }
}
