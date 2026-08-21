package Marshmello.MarshmelloWas.domain.checkin.adapter;

import Marshmello.MarshmelloWas.domain.checkin.port.ImageStorage;
import Marshmello.MarshmelloWas.global.exception.ApiException;
import Marshmello.MarshmelloWas.global.exception.ErrorCode;
import java.time.Instant;
import java.util.UUID;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

final class S3ImageStorage implements ImageStorage {

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3ImageStorageProperties properties;

    S3ImageStorage(
            S3Client s3Client,
            S3Presigner s3Presigner,
            S3ImageStorageProperties properties
    ) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.properties = properties;
    }

    @Override
    public StoredImage store(long userId, byte[] content, String contentType) {
        String resolvedContentType = contentType == null || contentType.isBlank()
                ? DEFAULT_CONTENT_TYPE
                : contentType;
        String objectKey = "users/%d/check-in-images/%s".formatted(userId, UUID.randomUUID());
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(properties.bucket())
                            .key(objectKey)
                            .contentType(resolvedContentType)
                            .build(),
                    RequestBody.fromBytes(content));
            return new StoredImage(objectKey, resolvedContentType);
        } catch (SdkException exception) {
            throw storageUnavailable(exception);
        }
    }

    @Override
    public ImageReadUrl createReadUrl(String objectKey) {
        try {
            PresignedGetObjectRequest request = s3Presigner.presignGetObject(
                    GetObjectPresignRequest.builder()
                            .signatureDuration(properties.presignedUrlTtl())
                            .getObjectRequest(GetObjectRequest.builder()
                                    .bucket(properties.bucket())
                                    .key(objectKey)
                                    .build())
                            .build());
            return new ImageReadUrl(
                    request.url().toURI(),
                    Instant.now().plus(properties.presignedUrlTtl()));
        } catch (Exception exception) {
            throw storageUnavailable(exception);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(objectKey)
                    .build());
        } catch (SdkException exception) {
            throw storageUnavailable(exception);
        }
    }

    private ApiException storageUnavailable(Exception cause) {
        ApiException exception = new ApiException(ErrorCode.IMAGE_STORAGE_UNAVAILABLE);
        exception.initCause(cause);
        return exception;
    }
}
