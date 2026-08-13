package Marshmello.MarshmelloWas.domain.user.dto;

import java.time.LocalDate;

public record UserProfileResDto(
        String nickname,
        LocalDate expectedDeliveryDate) {
}
