package org.scoula.global.file.controller;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;

import javax.validation.Valid;

import org.scoula.global.common.dto.ApiResponse;
import org.scoula.global.file.dto.S3Dto;
import org.scoula.global.file.service.S3ServiceInterface;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequestMapping("/api/s3")
@RequiredArgsConstructor
@Log4j2
public class S3TestControllerImpl implements S3TestController {

      private final S3ServiceInterface s3Service;

      /** {@inheritDoc} */
      @Override
      @PostMapping("/upload")
      public ResponseEntity<ApiResponse<S3Dto.UploadResponse>> uploadFile(
              @RequestParam("file") MultipartFile file) {
          try {
              String fileKey = s3Service.uploadFile(file);
              String fileUrl = s3Service.getFileUrl(fileKey);

              S3Dto.UploadResponse response =
                      S3Dto.UploadResponse.builder()
                              .fileKey(fileKey)
                              .fileUrl(fileUrl)
                              .originalFileName(file.getOriginalFilename())
                              .fileSize(file.getSize())
                              .contentType(file.getContentType())
                              .uploadedAt(LocalDateTime.now())
                              .build();

              return ResponseEntity.ok(ApiResponse.success(response, "S3 파일 업로드 성공"));
          } catch (Exception e) {
              log.error("S3 파일 업로드 실패", e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(ApiResponse.error("S3_ERROR", e.getMessage()));
          }
      }

      /** {@inheritDoc} */
      @Override
      @GetMapping("/download/{fileKey}")
      public ResponseEntity<byte[]> downloadFile(@PathVariable String fileKey) {
          try {
              InputStream s3Object = s3Service.downloadFile(fileKey);

              byte[] content = s3Object.readAllBytes();
              long fileSize = s3Service.getFileSize(fileKey);

              HttpHeaders headers = new HttpHeaders();
              headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
              headers.setContentLength(fileSize);

              // 파일 다운로드를 위한 헤더 설정
              String fileName = fileKey.substring(fileKey.lastIndexOf("/") + 1);
              headers.setContentDisposition(
                      org.springframework.http.ContentDisposition.attachment()
                              .filename(fileName)
                              .build());

              return ResponseEntity.ok().headers(headers).body(content);

          } catch (IOException e) {
              log.error("파일 다운로드 중 오류 발생: {}", e.getMessage(), e);
              return ResponseEntity.internalServerError().build();
          }
      }

      /** {@inheritDoc} */
      @Override
      @GetMapping("/info/{fileKey}")
      public ResponseEntity<ApiResponse<S3Dto.FileInfoResponse>> getFileInfo(
              @PathVariable String fileKey) {
          try {
              boolean exists = s3Service.fileExists(fileKey);
              String fileUrl = exists ? s3Service.getFileUrl(fileKey) : null;
              String presignedUrl = exists ? s3Service.generatePresignedUrl(fileKey, 60) : null;

              S3Dto.FileInfoResponse response =
                      S3Dto.FileInfoResponse.builder()
                              .fileKey(fileKey)
                              .fileUrl(fileUrl)
                              .presignedUrl(presignedUrl)
                              .exists(exists)
                              .queriedAt(LocalDateTime.now())
                              .build();

              return ResponseEntity.ok(ApiResponse.success(response, "S3 파일 정보 조회 성공"));
          } catch (Exception e) {
              log.error("S3 파일 정보 조회 실패", e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(ApiResponse.error("S3_ERROR", e.getMessage()));
          }
      }

      /** {@inheritDoc} */
      @Override
      @DeleteMapping("/delete")
      public ResponseEntity<ApiResponse<S3Dto.DeleteResponse>> deleteFile(
              @Valid @RequestBody S3Dto.DeleteRequest request) {
          try {
              s3Service.deleteFile(request.getFileKey());

              S3Dto.DeleteResponse response =
                      S3Dto.DeleteResponse.builder()
                              .fileKey(request.getFileKey())
                              .deleted(true)
                              .deletedAt(LocalDateTime.now())
                              .build();

              return ResponseEntity.ok(ApiResponse.success(response, "S3 파일 삭제 성공"));
          } catch (Exception e) {
              log.error("S3 파일 삭제 실패", e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(ApiResponse.error("S3_ERROR", e.getMessage()));
          }
      }

      /** {@inheritDoc} */
      @Override
      @PostMapping("/presigned-url")
      public ResponseEntity<ApiResponse<S3Dto.PresignedUrlResponse>> generatePresignedUrl(
              @Valid @RequestBody S3Dto.PresignedUrlRequest request) {
          try {
              // 유효 시간 검증 (1분 ~ 24시간)
              int durationMinutes = Math.max(1, Math.min(request.getDurationMinutes(), 1440));
              Duration duration = Duration.ofMinutes(durationMinutes);

              String presignedUrl =
                      s3Service.generatePresignedUrl(
                              request.getFileKey(), (int) duration.toMinutes());
              LocalDateTime now = LocalDateTime.now();

              S3Dto.PresignedUrlResponse response =
                      S3Dto.PresignedUrlResponse.builder()
                              .fileKey(request.getFileKey())
                              .presignedUrl(presignedUrl)
                              .expiresAt(now.plusMinutes(durationMinutes))
                              .generatedAt(now)
                              .build();

              return ResponseEntity.ok(ApiResponse.success(response, "S3 미리 서명된 URL 생성 성공"));
          } catch (Exception e) {
              log.error("S3 미리 서명된 URL 생성 실패", e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(ApiResponse.error("S3_ERROR", e.getMessage()));
          }
      }
}
