package com.wedding.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@RequiredArgsConstructor
public class R2Config {

    private final R2Properties r2Properties;

    private static final Region REGION = Region.of("auto");

    @Bean
    public S3Client r2S3Client() {
        String endpoint = String.format("https://%s.r2.cloudflarestorage.com", r2Properties.getAccountId());

        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(REGION)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                r2Properties.getAccessKeyId(),
                                r2Properties.getSecretAccessKey()
                        )
                ))
                .build();
    }

    @Bean
    public S3Presigner r2S3Presigner() {
        String endpoint = String.format("https://%s.r2.cloudflarestorage.com", r2Properties.getAccountId());

        return S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .region(REGION)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                r2Properties.getAccessKeyId(),
                                r2Properties.getSecretAccessKey()
                        )
                ))
                .build();
    }
}
