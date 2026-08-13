package Marshmello.MarshmelloWas.domain.user.controller;

import Marshmello.MarshmelloWas.domain.user.dto.UpdateUserProfileReqDto;
import Marshmello.MarshmelloWas.domain.user.dto.UserProfileResDto;
import Marshmello.MarshmelloWas.domain.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserProfileResDto getProfile() {
        return userService.getProfile();
    }

    @PatchMapping
    public UserProfileResDto updateProfile(
            @Valid @RequestBody UpdateUserProfileReqDto request) {
        return userService.updateProfile(request);
    }
}
