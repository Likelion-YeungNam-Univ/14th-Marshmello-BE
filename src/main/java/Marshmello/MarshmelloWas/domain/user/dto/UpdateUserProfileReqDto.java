package Marshmello.MarshmelloWas.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateUserProfileReqDto(
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 15, message = "닉네임은 2자 이상 15자 이하입니다.")
        String nickname,
        LocalDate expectedDeliveryDate) {
}
