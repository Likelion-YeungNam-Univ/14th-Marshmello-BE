package Marshmello.MarshmelloWas.domain.checkin.dto;

public record BodyDiaryResponse(
        short bodyRegion,
        Boolean stretchMark,
        String comment
) {
}
