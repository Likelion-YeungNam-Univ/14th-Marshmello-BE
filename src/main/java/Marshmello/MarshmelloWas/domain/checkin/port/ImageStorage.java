package Marshmello.MarshmelloWas.domain.checkin.port;

import java.net.URI;
import java.time.Instant;

public interface ImageStorage {

    StoredImage store(long userId, byte[] content, String contentType);

    ImageReadUrl createReadUrl(String objectKey);

    void delete(String objectKey);

    record StoredImage(String objectKey, String contentType) {
    }

    record ImageReadUrl(URI url, Instant expiresAt) {
    }
}
