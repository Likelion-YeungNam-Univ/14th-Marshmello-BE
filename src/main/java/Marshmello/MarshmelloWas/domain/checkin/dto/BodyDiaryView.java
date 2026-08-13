package Marshmello.MarshmelloWas.domain.checkin.dto;

import Marshmello.MarshmelloWas.domain.checkin.entity.CheckIn;

public record BodyDiaryView(short bodyRegion, Boolean stretchMark, String comment) {
}
