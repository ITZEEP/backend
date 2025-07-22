package org.scoula.global.file.dto;

import java.time.LocalDateTime;

import javax.validation.constraints.NotBlank;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class S3Dto {

      @Data
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      @ApiModel(value = "S3 파일 업로드 응답", description = "S3 파일 업로드 결과 정보")
      public static class UploadResponse {

          @ApiModelProperty(value = "파일 키", example = "uploads/uuid-filename.jpg")
          private String fileKey;

          @ApiModelProperty(
                  value = "파일 URL",
                  example = "https://bucket.s3.region.amazonaws.com/uploads/uuid-filename.jpg")
          private String fileUrl;

          @ApiModelProperty(value = "원본 파일명", example = "image.jpg")
          private String originalFileName;

          @ApiModelProperty(value = "파일 크기 (바이트)", example = "1024000")
          private Long fileSize;

          @ApiModelProperty(value = "콘텐츠 타입", example = "image/jpeg")
          private String contentType;

          @ApiModelProperty(value = "업로드 시간", example = "2024-07-20T14:30:00")
          private LocalDateTime uploadedAt;
      }

      @Data
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      @ApiModel(value = "S3 파일 다운로드 요청", description = "S3 파일 다운로드 요청 정보")
      public static class DownloadRequest {

          @NotBlank(message = "파일 키는 필수입니다")
          @ApiModelProperty(value = "파일 키", example = "uploads/uuid-filename.jpg", required = true)
          private String fileKey;
      }

      @Data
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      @ApiModel(value = "S3 파일 정보 응답", description = "S3 파일 정보")
      public static class FileInfoResponse {

          @ApiModelProperty(value = "파일 키", example = "uploads/uuid-filename.jpg")
          private String fileKey;

          @ApiModelProperty(
                  value = "파일 URL",
                  example = "https://bucket.s3.region.amazonaws.com/uploads/uuid-filename.jpg")
          private String fileUrl;

          @ApiModelProperty(
                  value = "미리 서명된 URL",
                  example =
                          "https://bucket.s3.region.amazonaws.com/uploads/uuid-filename.jpg?X-Amz-Algorithm=...")
          private String presignedUrl;

          @ApiModelProperty(value = "파일 존재 여부", example = "true")
          private Boolean exists;

          @ApiModelProperty(value = "조회 시간", example = "2024-07-20T14:30:00")
          private LocalDateTime queriedAt;
      }

      @Data
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      @ApiModel(value = "S3 파일 삭제 요청", description = "S3 파일 삭제 요청 정보")
      public static class DeleteRequest {

          @NotBlank(message = "파일 키는 필수입니다")
          @ApiModelProperty(value = "파일 키", example = "uploads/uuid-filename.jpg", required = true)
          private String fileKey;
      }

      @Data
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      @ApiModel(value = "S3 파일 삭제 응답", description = "S3 파일 삭제 결과")
      public static class DeleteResponse {

          @ApiModelProperty(value = "삭제된 파일 키", example = "uploads/uuid-filename.jpg")
          private String fileKey;

          @ApiModelProperty(value = "삭제 성공 여부", example = "true")
          private Boolean deleted;

          @ApiModelProperty(value = "삭제 시간", example = "2024-07-20T14:30:00")
          private LocalDateTime deletedAt;
      }

      @Data
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      @ApiModel(value = "S3 미리 서명된 URL 요청", description = "S3 미리 서명된 URL 생성 요청")
      public static class PresignedUrlRequest {

          @NotBlank(message = "파일 키는 필수입니다")
          @ApiModelProperty(value = "파일 키", example = "uploads/uuid-filename.jpg", required = true)
          private String fileKey;

          @ApiModelProperty(
                  value = "URL 유효 시간 (분)",
                  example = "60",
                  notes = "기본값: 60분, 최대: 1440분(24시간)")
          @Builder.Default
          private Integer durationMinutes = 60;
      }

      @Data
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      @ApiModel(value = "S3 미리 서명된 URL 응답", description = "S3 미리 서명된 URL 생성 결과")
      public static class PresignedUrlResponse {

          @ApiModelProperty(value = "파일 키", example = "uploads/uuid-filename.jpg")
          private String fileKey;

          @ApiModelProperty(
                  value = "미리 서명된 URL",
                  example =
                          "https://bucket.s3.region.amazonaws.com/uploads/uuid-filename.jpg?X-Amz-Algorithm=...")
          private String presignedUrl;

          @ApiModelProperty(value = "URL 만료 시간", example = "2024-07-20T15:30:00")
          private LocalDateTime expiresAt;

          @ApiModelProperty(value = "생성 시간", example = "2024-07-20T14:30:00")
          private LocalDateTime generatedAt;
      }
}
