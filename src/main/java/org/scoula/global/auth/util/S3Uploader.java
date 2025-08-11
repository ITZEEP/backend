package org.scoula.global.auth.util;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class S3Uploader {

      @Value("${cloud.aws.s3.bucket}")
      private String bucket;

      @Value("${cloud.aws.region.static}")
      private String region;

      @Value("${cloud.aws.credentials.access-key}")
      private String accessKey;

      @Value("${cloud.aws.credentials.secret-key}")
      private String secretKey;

      private S3Client s3Client;

      @PostConstruct
      public void init() {
          s3Client =
                  S3Client.builder()
                          .region(Region.of(region))
                          .credentialsProvider(
                                  StaticCredentialsProvider.create(
                                          AwsBasicCredentials.create(accessKey, secretKey)))
                          .build();
      }

      public String upload(MultipartFile file, String dirName) {
          String originalFileName = file.getOriginalFilename();
          String fileName = buildFileName(dirName, originalFileName);

          try {
              PutObjectRequest putObjectRequest =
                      PutObjectRequest.builder()
                              .bucket(bucket)
                              .key(fileName)
                              .acl(ObjectCannedACL.PUBLIC_READ)
                              .contentType(file.getContentType())
                              .build();

              s3Client.putObject(
                      putObjectRequest,
                      software.amazon.awssdk.core.sync.RequestBody.fromBytes(file.getBytes()));

              return getFileUrl(fileName);
          } catch (IOException e) {
              log.error("S3 파일 업로드 실패", e);
              throw new RuntimeException("S3 파일 업로드 실패", e);
          }
      }

      private String buildFileName(String dirName, String originalFileName) {
          String uuid = UUID.randomUUID().toString();
          String timestamp =
                  LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
          return dirName + "/" + timestamp + "_" + uuid + "_" + originalFileName;
      }

      private String getFileUrl(String fileName) {
          return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + fileName;
      }

      public void delete(String fileName) {
          try {
              DeleteObjectRequest deleteObjectRequest =
                      DeleteObjectRequest.builder().bucket(bucket).key(fileName).build();

              s3Client.deleteObject(deleteObjectRequest);
          } catch (S3Exception e) {
              log.error("S3 파일 삭제 실패: {}", fileName, e);
              throw new RuntimeException("S3 파일 삭제 실패", e);
          }
      }
}
