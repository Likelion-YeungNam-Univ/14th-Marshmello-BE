package Marshmello.MarshmelloWas.domain.user.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void createsPendingProfileWithDefaultNickname() {
        User user = User.createPendingProfile();

        assertThat(user.getNickname()).isEqualTo("마시멜로");
        assertThat(user.getExpectedDeliveryDate()).isNull();
        assertThat(user.isProfileCompleted()).isFalse();
    }

    @Test
    void updatesProfileState() {
        User user = new User("기존닉네임", LocalDate.of(2026, 8, 20));
        LocalDate updatedDeliveryDate = LocalDate.of(2026, 9, 1);

        user.updateProfile("새닉네임", updatedDeliveryDate);

        assertThat(user.getNickname()).isEqualTo("새닉네임");
        assertThat(user.getExpectedDeliveryDate()).isEqualTo(updatedDeliveryDate);
        assertThat(user.isProfileCompleted()).isTrue();
    }
}
