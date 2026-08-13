package Marshmello.MarshmelloWas.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import Marshmello.MarshmelloWas.domain.auth.port.CurrentUserIdProvider;
import Marshmello.MarshmelloWas.domain.user.dto.UpdateUserProfileReqDto;
import Marshmello.MarshmelloWas.domain.user.dto.UserProfileResDto;
import Marshmello.MarshmelloWas.domain.user.entity.User;
import Marshmello.MarshmelloWas.domain.user.repository.UserRepository;
import Marshmello.MarshmelloWas.global.exception.ApiException;
import Marshmello.MarshmelloWas.global.exception.ErrorCode;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UserServiceTest {

    private static final long USER_ID = 1L;

    private final UserRepository userRepository = mock(UserRepository.class);
    private final CurrentUserIdProvider currentUserIdProvider = () -> USER_ID;
    private final UserService userService = new UserService(userRepository, currentUserIdProvider);

    @Test
    void returnsCurrentUserProfile() {
        LocalDate expectedDeliveryDate = LocalDate.of(2026, 9, 1);
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(new User("마시멜로", expectedDeliveryDate)));

        UserProfileResDto response = userService.getProfile();

        assertThat(response.nickname()).isEqualTo("마시멜로");
        assertThat(response.expectedDeliveryDate()).isEqualTo(expectedDeliveryDate);
        assertThat(response.profileCompleted()).isTrue();
    }

    @Test
    void updatesCurrentUserProfile() {
        User user = new User("기존닉네임", LocalDate.of(2026, 8, 20));
        LocalDate updatedDeliveryDate = LocalDate.of(2026, 9, 1);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        UserProfileResDto response = userService.updateProfile(
                new UpdateUserProfileReqDto("새닉네임", updatedDeliveryDate));

        assertThat(user.getNickname()).isEqualTo("새닉네임");
        assertThat(user.getExpectedDeliveryDate()).isEqualTo(updatedDeliveryDate);
        assertThat(response.nickname()).isEqualTo("새닉네임");
        assertThat(response.expectedDeliveryDate()).isEqualTo(updatedDeliveryDate);
        assertThat(response.profileCompleted()).isTrue();
    }

    @Test
    void rejectsMissingCurrentUser() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(userService::getProfile)
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
    }
}
