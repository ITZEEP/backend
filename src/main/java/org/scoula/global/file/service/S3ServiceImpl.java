package org.scoula.global.file.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.UUID;

import org.scoula.global.common.exception.BusinessException;
import org.scoula.global.common.service.AbstractExternalService;
import org.scoula.global.file.exception.S3ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

/**
 * AWS S3 파일 저장소 서비스 구현체
 *
 * <p>AWS S3를 이용한 파일 업로드, 다운로드, 삭제 기능을 구현합니다. 미리 서명된 URL 생성 및 파일 존재 여부 확인 기능도 제공합니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class S3ServiceImpl extends AbstractExternalService implements S3ServiceInterface {

      private final S3Client s3Client;

      @Value("${aws.s3.bucket-name}")
      private String bucketName;

      /** {@inheritDoc} */
      @Override
      public String uploadFile(MultipartFile file) {
          validateFile(file);

          String fileName = generateFileName(file.getOriginalFilename());
          String key = "uploads/" + fileName;

          return executeSafely(
                  () -> {
                      try {
                          PutObjectRequest putObjectRequest =
                                  PutObjectRequest.builder()
                                          .bucket(bucketName)
                                          .key(key)
                                          .contentType(file.getContentType())
                                          .contentLength(file.getSize())
                                          .build();

                          s3Client.putObject(
                                  putObjectRequest,
                                  RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

                          return key;
                      } catch (Exception e) {
                          throw new BusinessException(
                                  S3ErrorCode.FILE_UPLOAD_FAILED, "S3 파일 업로드에 실패했습니다", e);
                      }
                  },
                  "S3 파일 업로드");
      }

      /** {@inheritDoc} */
      @Override
      public String uploadFile(MultipartFile file, String fileName) {
          validateFile(file);

          if (fileName == null || fileName.trim().isEmpty()) {
              throw new BusinessException(S3ErrorCode.INVALID_FILE_NAME, "파일명이 올바르지 않습니다");
          }

          String key = "uploads/" + generateFileName(fileName);

          return executeSafely(
                  () -> {
                      try {
                          PutObjectRequest putObjectRequest =
                                  PutObjectRequest.builder()
                                          .bucket(bucketName)
                                          .key(key)
                                          .contentType(file.getContentType())
                                          .contentLength(file.getSize())
                                          .build();

                          s3Client.putObject(
                                  putObjectRequest,
                                  RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

                          return key;
                      } catch (Exception e) {
                          throw new BusinessException(
                                  S3ErrorCode.FILE_UPLOAD_FAILED, "S3 파일 업로드에 실패했습니다", e);
                      }
                  },
                  "S3 파일 업로드");
      }

      /**
       * InputStream을 이용한 파일 업로드 (내부 메서드)
       *
       * @param fileName 파일명
       * @param inputStream 파일 입력 스트림
       * @param contentLength 파일 크기
       * @param contentType 파일 컨텐츠 타입
       * @return 업로드된 파일의 S3 키
       */
      private String uploadFile(
              String fileName, InputStream inputStream, long contentLength, String contentType) {
          if (fileName == null || fileName.trim().isEmpty()) {
              throw new BusinessException(S3ErrorCode.INVALID_FILE_NAME, "파일명이 올바르지 않습니다");
          }

          String key = "uploads/" + generateFileName(fileName);

          return executeSafely(
                  () -> {
                      try {
                          PutObjectRequest putObjectRequest =
                                  PutObjectRequest.builder()
                                          .bucket(bucketName)
                                          .key(key)
                                          .contentType(contentType)
                                          .contentLength(contentLength)
                                          .build();

                          s3Client.putObject(
                                  putObjectRequest,
                                  RequestBody.fromInputStream(inputStream, contentLength));

                          return key;
                      } catch (Exception e) {
                          throw new BusinessException(
                                  S3ErrorCode.FILE_UPLOAD_FAILED, "S3 파일 업로드에 실패했습니다", e);
                      }
                  },
                  "S3 파일 업로드");
      }

      /** {@inheritDoc} */
      @Override
      public InputStream downloadFile(String key) {
          return executeSafely(
                  () -> {
                      try {
                          GetObjectRequest getObjectRequest =
                                  GetObjectRequest.builder().bucket(bucketName).key(key).build();

                          return s3Client.getObject(getObjectRequest);
                      } catch (NoSuchKeyException e) {
                          throw new BusinessException(S3ErrorCode.FILE_NOT_FOUND, "파일을 찾을 수 없습니다", e);
                      } catch (Exception e) {
                          throw new BusinessException(
                                  S3ErrorCode.FILE_NOT_FOUND, "파일 다운로드에 실패했습니다", e);
                      }
                  },
                  "S3 파일 다운로드");
      }

      /** {@inheritDoc} */
      @Override
      public String generatePresignedUrl(String key, int durationMinutes) {
          Duration duration = Duration.ofMinutes(durationMinutes);
          return executeSafely(
                  () -> {
                      try (S3Presigner presigner = S3Presigner.create()) {
                          GetObjectRequest getObjectRequest =
                                  GetObjectRequest.builder().bucket(bucketName).key(key).build();

                          GetObjectPresignRequest presignRequest =
                                  GetObjectPresignRequest.builder()
                                          .signatureDuration(duration)
                                          .getObjectRequest(getObjectRequest)
                                          .build();

                          PresignedGetObjectRequest presignedRequest =
                                  presigner.presignGetObject(presignRequest);
                          return presignedRequest.url().toString();
                      } catch (Exception e) {
                          throw new BusinessException(
                                  S3ErrorCode.FILE_NOT_FOUND, "미리 서명된 URL 생성에 실패했습니다", e);
                      }
                  },
                  "S3 미리 서명된 URL 생성");
      }

      /** {@inheritDoc} */
      @Override
      public String getFileUrl(String key) {
          return executeSafely(
                  () -> {
                      try {
                          GetUrlRequest request =
                                  GetUrlRequest.builder().bucket(bucketName).key(key).build();

                          URL url = s3Client.utilities().getUrl(request);
                          return url.toString();
                      } catch (Exception e) {
                          throw new BusinessException(
                                  S3ErrorCode.FILE_NOT_FOUND, "파일 URL 생성에 실패했습니다", e);
                      }
                  },
                  "S3 파일 URL 생성");
      }

      /** {@inheritDoc} */
      @Override
      public boolean fileExists(String key) {
          return executeSafely(
                  () -> {
                      try {
                          HeadObjectRequest headObjectRequest =
                                  HeadObjectRequest.builder().bucket(bucketName).key(key).build();

                          s3Client.headObject(headObjectRequest);
                          return true;
                      } catch (NoSuchKeyException e) {
                          return false;
                      } catch (Exception e) {
                          throw new BusinessException(
                                  S3ErrorCode.S3_CONNECTION_FAILED, "파일 존재 확인에 실패했습니다", e);
                      }
                  },
                  "S3 파일 존재 확인");
      }

      /** {@inheritDoc} */
      @Override
      public boolean deleteFile(String key) {
          return executeSafely(
                  () -> {
                      try {
                          DeleteObjectRequest deleteObjectRequest =
                                  DeleteObjectRequest.builder().bucket(bucketName).key(key).build();

                          s3Client.deleteObject(deleteObjectRequest);
                          return true;
                      } catch (Exception e) {
                          log.error("파일 삭제 실패: {}", key, e);
                          return false;
                      }
                  },
                  "S3 파일 삭제");
      }

      /** {@inheritDoc} */
      @Override
      public long getFileSize(String key) {
          return executeSafely(
                  () -> {
                      try {
                          HeadObjectRequest headObjectRequest =
                                  HeadObjectRequest.builder().bucket(bucketName).key(key).build();

                          return s3Client.headObject(headObjectRequest).contentLength();
                      } catch (NoSuchKeyException e) {
                          throw new BusinessException(S3ErrorCode.FILE_NOT_FOUND, "파일을 찾을 수 없습니다", e);
                      } catch (Exception e) {
                          throw new BusinessException(
                                  S3ErrorCode.S3_CONNECTION_FAILED, "파일 크기 조회에 실패했습니다", e);
                      }
                  },
                  "S3 파일 크기 조회");
      }

      private void validateFile(MultipartFile file) {
          if (file == null || file.isEmpty()) {
              throw new BusinessException(S3ErrorCode.EMPTY_FILE, "빈 파일은 업로드할 수 없습니다");
          }

          if (file.getOriginalFilename() == null || file.getOriginalFilename().trim().isEmpty()) {
              throw new BusinessException(S3ErrorCode.INVALID_FILE_NAME, "파일명이 올바르지 않습니다");
          }

          // 파일 크기 제한 (50MB)
          if (file.getSize() > 50 * 1024 * 1024) {
              throw new BusinessException(S3ErrorCode.FILE_SIZE_EXCEEDED, "파일 크기가 50MB를 초과했습니다");
          }
      }

      private String generateFileName(String originalFilename) {
          String extension = "";
          if (originalFilename != null && originalFilename.contains(".")) {
              extension = originalFilename.substring(originalFilename.lastIndexOf("."));
          }
          return UUID.randomUUID().toString() + extension;
      }

      /** {@inheritDoc} */
      @Override
      public String uploadProfileImageFromUrl(String imageUrl, Long userId) {
          if (imageUrl == null || imageUrl.trim().isEmpty()) {
              log.warn("프로필 이미지 URL이 null이거나 비어있음");
              return null;
          }

          log.info("프로필 이미지 업로드 시작 - URL: {}, User ID: {}", imageUrl, userId);

          return executeSafely(
                  () -> {
                      try {
                          // URL에서 이미지 다운로드
                          URL url = new URL(imageUrl);
                          log.info("이미지 다운로드 시작 - URL: {}", imageUrl);

                          HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                          connection.setRequestMethod("GET");
                          connection.setConnectTimeout(5000);
                          connection.setReadTimeout(5000);
                          connection.setRequestProperty("User-Agent", "Mozilla/5.0");

                          int responseCode = connection.getResponseCode();
                          log.info("HTTP 응답 코드: {}", responseCode);

                          // 이미지 데이터 읽기
                          try (InputStream inputStream = connection.getInputStream();
                                  ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                              byte[] buffer = new byte[4096];
                              int bytesRead;
                              while ((bytesRead = inputStream.read(buffer)) != -1) {
                                  outputStream.write(buffer, 0, bytesRead);
                              }

                              byte[] imageBytes = outputStream.toByteArray();
                              long contentLength = imageBytes.length;
                              log.info("다운로드한 이미지 크기: {} bytes", contentLength);

                              // Content-Type 확인
                              String contentType = connection.getContentType();
                              log.info("Content-Type: {}", contentType);

                              if (contentType == null || !contentType.startsWith("image/")) {
                                  contentType = "image/jpeg"; // 기본값
                                  log.warn("Content-Type이 이미지가 아니므로 기본값 사용: {}", contentType);
                              }

                              // 파일 확장자 결정
                              String extension = ".jpg"; // 기본값
                              if (contentType.contains("png")) {
                                  extension = ".png";
                              } else if (contentType.contains("gif")) {
                                  extension = ".gif";
                              } else if (contentType.contains("webp")) {
                                  extension = ".webp";
                              }

                              // S3 키 생성 (profile-images/userId/UUID.확장자)
                              String fileName = UUID.randomUUID().toString() + extension;
                              String key = "profile-images/" + userId + "/" + fileName;

                              // S3에 업로드
                              log.info("S3 업로드 시작 - Bucket: {}, Key: {}", bucketName, key);

                              PutObjectRequest putObjectRequest =
                                      PutObjectRequest.builder()
                                              .bucket(bucketName)
                                              .key(key)
                                              .contentType(contentType)
                                              .contentLength(contentLength)
                                              .build();

                              s3Client.putObject(
                                      putObjectRequest,
                                      RequestBody.fromInputStream(
                                              new ByteArrayInputStream(imageBytes), contentLength));

                              log.info("S3 업로드 완료");

                              // S3 URL 반환
                              String s3Url = getFileUrl(key);
                              log.info("생성된 S3 URL: {}", s3Url);
                              return s3Url;
                          }
                      } catch (Exception e) {
                          log.error("프로필 이미지 업로드 실패: {}", imageUrl, e);
                          return null; // 프로필 이미지 업로드 실패 시 null 반환 (회원가입은 계속 진행)
                      }
                  },
                  "프로필 이미지 S3 업로드");
      }
}
