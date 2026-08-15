package Marshmello.MarshmelloWas.domain.checkin.adapter;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.storage.s3")
public record S3ImageStorageProperties(
        String bucket,
        String region,
        Duration presignedUrlTtl
) {
}
