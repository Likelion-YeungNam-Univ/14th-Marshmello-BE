package Marshmello.MarshmelloWas.domain.checkin.adapter;

import Marshmello.MarshmelloWas.domain.checkin.port.ImageStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@ConditionalOnProperty(prefix = "app.storage.s3", name = "bucket", matchIfMissing = false)
public class S3ImageStorageConfiguration {

    @Bean
    S3Client s3Client(S3ImageStorageProperties properties) {
        return S3Client.builder()
                .region(Region.of(properties.region()))
                .build();
    }

    @Bean
    S3Presigner s3Presigner(S3ImageStorageProperties properties) {
        return S3Presigner.builder()
                .region(Region.of(properties.region()))
                .build();
    }

    @Bean
    ImageStorage imageStorage(
            S3Client s3Client,
            S3Presigner s3Presigner,
            S3ImageStorageProperties properties
    ) {
        return new S3ImageStorage(s3Client, s3Presigner, properties);
    }
}
