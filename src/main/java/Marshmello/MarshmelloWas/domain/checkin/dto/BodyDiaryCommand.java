package Marshmello.MarshmelloWas.domain.checkin.dto;

import Marshmello.MarshmelloWas.domain.checkin.entity.CheckIn;

public record BodyDiaryCommand(short bodyRegion, Boolean stretchMark, String comment) {
}
