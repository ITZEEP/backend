package org.scoula.domain.mypage.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.scoula.domain.mypage.exception.MyPageErrorCode;
import org.scoula.global.common.exception.BusinessException;
import org.scoula.global.file.service.S3ServiceInterface;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * 프로필 이미지 관리 서비스 구현체
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class ProfileImageServiceImpl implements ProfileImageService {

      private final S3ServiceInterface s3Service;

      @Value("${aws.s3.bucket-name}")
      private String bucketName;

      @Value("${aws.s3.region:ap-northeast-2}")
      private String awsRegion;

      // 허용된 이미지 확장자
      private static final List<String> ALLOWED_EXTENSIONS =
              Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp");

      // 프로필 이미지 최대 크기 (10MB)
      private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

      @Override
      public String uploadProfileImage(MultipartFile file, Long userId, String previousImageUrl) {
          validateProfileImage(file);

          if (userId == null) {
              throw new BusinessException(MyPageErrorCode.USER_NOT_FOUND);
          }

          // 이전 이미지 삭제
          if (previousImageUrl != null && !previousImageUrl.isEmpty()) {
              try {
                  String previousKey = extractKeyFromUrl(previousImageUrl);
                  if (previousKey != null && previousKey.startsWith("profile-images/")) {
                      s3Service.deleteFile(previousKey);
                      log.info("이전 프로필 이미지 삭제 완료: {}", previousKey);
                  }
              } catch (Exception e) {
                  log.warn("이전 프로필 이미지 삭제 실패: {}", previousImageUrl, e);
                  // 삭제 실패는 무시하고 계속 진행
              }
          }

          // 새 이미지 업로드
          String originalFilename = file.getOriginalFilename();
          String extension = extractExtension(originalFilename);
          String fileName = UUID.randomUUID().toString() + extension;
          String key = "profile-images/" + userId + "/" + fileName;

          try {
              // S3ServiceInterface의 uploadFile 메서드 사용
              String uploadedKey = s3Service.uploadFile(file, fileName);

              // 업로드된 파일을 profile-images 디렉토리로 이동하려면
              // S3 서비스에서 직접 처리하도록 수정 필요
              // 현재는 기본 uploads/ 디렉토리를 사용

              String s3Url = s3Service.getFileUrl(uploadedKey);
              log.info("프로필 이미지 업로드 완료 - userId: {}, S3 URL: {}", userId, s3Url);
              return s3Url;
          } catch (Exception e) {
              log.error("프로필 이미지 업로드 실패 - userId: {}", userId, e);
              throw new BusinessException(MyPageErrorCode.IMAGE_UPLOAD_FAILED);
          }
      }

      @Override
      public String uploadProfileImageFromUrl(String imageUrl, Long userId) {
          if (imageUrl == null || imageUrl.trim().isEmpty()) {
              log.warn("프로필 이미지 URL이 null이거나 비어있음");
              return null;
          }

          if (userId == null) {
              log.warn("사용자 ID가 null");
              return null;
          }

          log.info("프로필 이미지 업로드 시작 - URL: {}, User ID: {}", imageUrl, userId);

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

              if (responseCode != HttpURLConnection.HTTP_OK) {
                  log.warn("이미지 다운로드 실패 - HTTP 응답 코드: {}", responseCode);
                  return null;
              }

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

                  // 파일 크기 검증
                  if (contentLength > MAX_FILE_SIZE) {
                      log.warn("이미지 크기가 제한을 초과함: {} bytes", contentLength);
                      return null;
                  }

                  // Content-Type 확인
                  String contentType = connection.getContentType();
                  log.info("Content-Type: {}", contentType);

                  if (contentType == null || !contentType.startsWith("image/")) {
                      contentType = "image/jpeg"; // 기본값
                      log.warn("Content-Type이 이미지가 아니므로 기본값 사용: {}", contentType);
                  }

                  // 파일 확장자 결정
                  String extension = determineExtension(contentType);

                  // 파일명 생성
                  String fileName = UUID.randomUUID().toString() + extension;
                  String key = "profile-images/" + userId + "/" + fileName;

                  // ByteArrayInputStream을 MultipartFile로 변환하여 S3에 업로드
                  // S3ServiceInterface는 MultipartFile을 받으므로 직접 업로드 구현
                  try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
                      // S3Service의 기본 uploadFile 메서드는 MultipartFile을 받으므로
                      // 여기서는 직접 S3 URL을 생성하거나 S3Service에 InputStream을 받는 메서드 추가 필요
                      // 현재는 간단한 해결책으로 null 반환
                      log.warn("외부 URL 이미지 업로드는 S3Service에서 직접 처리하도록 위임");
                      return null; // S3Service의 uploadProfileImageFromUrl을 계속 사용
                  }
              }
          } catch (Exception e) {
              log.error("프로필 이미지 업로드 실패: {}", imageUrl, e);
              return null; // 프로필 이미지 업로드 실패 시 null 반환
          }
      }

      @Override
      public boolean deleteProfileImage(String imageUrl) {
          if (imageUrl == null || imageUrl.isEmpty()) {
              return false;
          }

          try {
              String key = extractKeyFromUrl(imageUrl);
              if (key != null && key.startsWith("profile-images/")) {
                  return s3Service.deleteFile(key);
              }
              return false;
          } catch (Exception e) {
              log.error("프로필 이미지 삭제 실패: {}", imageUrl, e);
              return false;
          }
      }

      @Override
      public void validateProfileImage(MultipartFile file) {
          if (file == null || file.isEmpty()) {
              throw new BusinessException(MyPageErrorCode.EMPTY_IMAGE_FILE);
          }

          String originalFilename = file.getOriginalFilename();
          if (originalFilename == null || originalFilename.trim().isEmpty()) {
              throw new BusinessException(MyPageErrorCode.INVALID_IMAGE_NAME);
          }

          // 파일 확장자 검증
          String extension = extractExtension(originalFilename);
          if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
              throw new BusinessException(MyPageErrorCode.INVALID_IMAGE_FORMAT);
          }

          // 파일 크기 검증
          if (file.getSize() > MAX_FILE_SIZE) {
              throw new BusinessException(MyPageErrorCode.IMAGE_SIZE_EXCEEDED);
          }

          // Content-Type 검증
          String contentType = file.getContentType();
          if (contentType == null || !contentType.startsWith("image/")) {
              throw new BusinessException(MyPageErrorCode.INVALID_IMAGE_FORMAT);
          }
      }

      /** 파일명에서 확장자를 추출합니다. */
      private String extractExtension(String filename) {
          if (filename != null && filename.contains(".")) {
              return filename.substring(filename.lastIndexOf("."));
          }
          return ".jpg"; // 기본값
      }

      /** Content-Type으로부터 파일 확장자를 결정합니다. */
      private String determineExtension(String contentType) {
          if (contentType == null) {
              return ".jpg";
          }

          if (contentType.contains("png")) {
              return ".png";
          } else if (contentType.contains("gif")) {
              return ".gif";
          } else if (contentType.contains("webp")) {
              return ".webp";
          }
          return ".jpg"; // 기본값
      }

      /** S3 URL에서 키를 추출합니다. */
      private String extractKeyFromUrl(String url) {
          if (url == null || url.isEmpty()) {
              return null;
          }

          try {
              // https://itjib-bucket.s3.{region}.amazonaws.com/profile-images/11/uuid.jpg
              // -> profile-images/11/uuid.jpg
              String bucketUrl = "https://" + bucketName + ".s3." + awsRegion + ".amazonaws.com/";
              if (url.startsWith(bucketUrl)) {
                  return url.substring(bucketUrl.length());
              }

              // CloudFront URL 등 다른 형식도 처리 가능
              // https://d1234567890.cloudfront.net/profile-images/11/uuid.jpg
              if (url.contains("/profile-images/")) {
                  int index = url.indexOf("/profile-images/");
                  return url.substring(index + 1);
              }

              return null;
          } catch (Exception e) {
              log.warn("URL에서 키 추출 실패: {}", url, e);
              return null;
          }
      }
}
