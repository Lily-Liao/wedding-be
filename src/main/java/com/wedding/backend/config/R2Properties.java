package com.wedding.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "cloudflare.r2")
public class R2Properties {

    private String accountId;
    private String accessKeyId;
    private String secretAccessKey;
    private String bucketName;
    private String publicUrl;
    private int presignExpiryMinutes = 60;
}
