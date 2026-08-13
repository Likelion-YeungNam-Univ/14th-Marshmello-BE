package Marshmello.MarshmelloWas.domain.checkin.dto;

import Marshmello.MarshmelloWas.domain.checkin.entity.CheckIn;

public record CheckInImageReference(Long checkInId, Long imageId) {
}
