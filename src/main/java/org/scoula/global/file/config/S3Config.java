package org.scoula.global.file.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/** AWS S3 설정 클래스 */
@Configuration
public class S3Config {

      @Value("${aws.s3.access-key}")
      private String accessKey;

      @Value("${aws.s3.secret-key}")
      private String secretKey;

      @Value("${aws.s3.region}")
      private String region;

      @Bean
      public S3Client s3Client() {
          // 빈 문자열 또는 null인 경우 기본 자격 증명 공급자 사용
          if (accessKey == null || accessKey.isEmpty() || secretKey == null || secretKey.isEmpty()) {
              return S3Client.builder().region(Region.of(region)).build();
          }

          AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);

          return S3Client.builder()
                  .region(Region.of(region))
                  .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                  .build();
      }
}
