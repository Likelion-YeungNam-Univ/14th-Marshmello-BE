package Marshmello.MarshmelloWas.domain.checkin.dto;

import java.net.URI;
import java.time.Instant;

public record ImageUrlResponse(
        long imageId,
        URI url,
        Instant expiresAt
) {
}
