package Marshmello.MarshmelloWas.domain.checkin.adapter;

import Marshmello.MarshmelloWas.domain.checkin.port.ImageStorage;
import Marshmello.MarshmelloWas.global.exception.ApiException;
import Marshmello.MarshmelloWas.global.exception.ErrorCode;
import org.springframework.context.annotation.Fallback;
import org.springframework.stereotype.Component;

@Component
@Fallback
public class UnavailableImageStorage implements ImageStorage {

    @Override
    public StoredImage store(long userId, byte[] content, String contentType) {
        throw unavailable();
    }

    @Override
    public ImageReadUrl createReadUrl(String objectKey) {
        throw unavailable();
    }

    @Override
    public void delete(String objectKey) {
        throw unavailable();
    }

    private ApiException unavailable() {
        return new ApiException(ErrorCode.IMAGE_STORAGE_UNAVAILABLE);
    }
}
