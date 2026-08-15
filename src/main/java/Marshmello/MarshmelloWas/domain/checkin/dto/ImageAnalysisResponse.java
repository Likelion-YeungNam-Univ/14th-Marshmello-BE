package Marshmello.MarshmelloWas.domain.checkin.dto;

public record ImageAnalysisResponse(
        boolean detected,
        Long imageId,
        Short score
) {
}
