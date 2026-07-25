package com.clientdesk.attachment;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "clientdesk.attachments",
        name = "storage-provider",
        havingValue = "cloudinary"
)
public class CloudinaryAttachmentConfiguration {

    @Bean
    Cloudinary cloudinary(AttachmentProperties properties) {
        AttachmentProperties.Cloudinary settings = properties.getCloudinary();
        requireCredential(settings.getCloudName(), "CLOUDINARY_CLOUD_NAME");
        requireCredential(settings.getApiKey(), "CLOUDINARY_API_KEY");
        requireCredential(settings.getApiSecret(), "CLOUDINARY_API_SECRET");

        Map<String, Object> configuration = ObjectUtils.asMap(
                "cloud_name", settings.getCloudName(),
                "api_key", settings.getApiKey(),
                "api_secret", settings.getApiSecret(),
                "secure", true
        );
        return new Cloudinary(configuration);
    }

    @Bean
    AttachmentStorage cloudinaryAttachmentStorage(
            Cloudinary cloudinary,
            AttachmentProperties properties
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getCloudinary().getDownloadTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        return new CloudinaryAttachmentStorage(cloudinary, properties, httpClient);
    }

    private void requireCredential(String value, String environmentVariable) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(environmentVariable + " is required for Cloudinary attachment storage");
        }
    }
}
