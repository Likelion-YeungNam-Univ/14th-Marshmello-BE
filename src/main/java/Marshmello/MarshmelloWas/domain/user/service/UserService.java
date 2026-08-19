package Marshmello.MarshmelloWas.domain.user.service;

import Marshmello.MarshmelloWas.domain.auth.port.CurrentUserIdProvider;
import Marshmello.MarshmelloWas.domain.user.dto.UpdateUserProfileReqDto;
import Marshmello.MarshmelloWas.domain.user.dto.UserProfileResDto;
import Marshmello.MarshmelloWas.domain.user.entity.User;
import Marshmello.MarshmelloWas.domain.user.repository.UserRepository;
import Marshmello.MarshmelloWas.global.exception.ApiException;
import Marshmello.MarshmelloWas.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final CurrentUserIdProvider currentUserIdProvider;

    public UserService(UserRepository userRepository, CurrentUserIdProvider currentUserIdProvider) {
        this.userRepository = userRepository;
        this.currentUserIdProvider = currentUserIdProvider;
    }

    public UserProfileResDto getProfile() {
        return toResponse(getCurrentUser());
    }

    @Transactional
    public UserProfileResDto updateProfile(UpdateUserProfileReqDto request) {
        User user = getCurrentUser();
        user.updateProfile(request.nickname(), request.expectedDeliveryDate());
        return toResponse(user);
    }

    private User getCurrentUser() {
        long userId = currentUserIdProvider.requireCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }

    private static UserProfileResDto toResponse(User user) {
        return new UserProfileResDto(
                user.getNickname(),
                user.getExpectedDeliveryDate(),
                user.isProfileCompleted());
    }

    @Transactional
    public void deleteUser() {
        userRepository.delete(getCurrentUser());
    }
}
