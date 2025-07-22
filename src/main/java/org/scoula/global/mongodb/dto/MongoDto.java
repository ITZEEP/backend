package org.scoula.global.mongodb.dto;

import java.time.LocalDateTime;
import java.util.Map;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MongoDB 관련 DTO 클래스들
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
public class MongoDto {

      /** 문서 저장 요청 DTO */
      @Data
      @Builder
      @NoArgsConstructor(access = AccessLevel.PROTECTED)
      @AllArgsConstructor(access = AccessLevel.PRIVATE)
      public static class SaveDocumentRequest {

          @NotBlank(message = "컬렉션명은 필수입니다")
          @Size(max = 100, message = "컬렉션명은 100자를 초과할 수 없습니다")
          private String collectionName;

          private Map<String, Object> document;
      }

      /** 문서 저장 응답 DTO */
      @Data
      @Builder
      @NoArgsConstructor(access = AccessLevel.PROTECTED)
      @AllArgsConstructor(access = AccessLevel.PRIVATE)
      public static class SaveDocumentResponse {

          private String id;
          private String collectionName;
          private Map<String, Object> document;

          @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
          private LocalDateTime savedAt;
      }

      /** 문서 조회 요청 DTO */
      @Data
      @Builder
      @NoArgsConstructor(access = AccessLevel.PROTECTED)
      @AllArgsConstructor(access = AccessLevel.PRIVATE)
      public static class FindDocumentRequest {

          @NotBlank(message = "컬렉션명은 필수입니다")
          @Size(max = 100, message = "컬렉션명은 100자를 초과할 수 없습니다")
          private String collectionName;

          private String id;
          private Map<String, Object> query;
          private Integer limit;
          private Integer skip;
      }

      /** 문서 조회 응답 DTO */
      @Data
      @Builder
      @NoArgsConstructor(access = AccessLevel.PROTECTED)
      @AllArgsConstructor(access = AccessLevel.PRIVATE)
      public static class FindDocumentResponse {

          private String collectionName;
          private long totalCount;
          private java.util.List<Map<String, Object>> documents;

          @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
          private LocalDateTime queriedAt;
      }

      /** 문서 업데이트 요청 DTO */
      @Data
      @Builder
      @NoArgsConstructor(access = AccessLevel.PROTECTED)
      @AllArgsConstructor(access = AccessLevel.PRIVATE)
      public static class UpdateDocumentRequest {

          @NotBlank(message = "컬렉션명은 필수입니다")
          @Size(max = 100, message = "컬렉션명은 100자를 초과할 수 없습니다")
          private String collectionName;

          @NotBlank(message = "문서 ID는 필수입니다")
          private String id;

          private Map<String, Object> document;
      }

      /** 문서 업데이트 응답 DTO */
      @Data
      @Builder
      @NoArgsConstructor(access = AccessLevel.PROTECTED)
      @AllArgsConstructor(access = AccessLevel.PRIVATE)
      public static class UpdateDocumentResponse {

          private String id;
          private String collectionName;
          private Map<String, Object> document;

          @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
          private LocalDateTime updatedAt;
      }

      /** 문서 삭제 요청 DTO */
      @Data
      @Builder
      @NoArgsConstructor(access = AccessLevel.PROTECTED)
      @AllArgsConstructor(access = AccessLevel.PRIVATE)
      public static class DeleteDocumentRequest {

          @NotBlank(message = "컬렉션명은 필수입니다")
          @Size(max = 100, message = "컬렉션명은 100자를 초과할 수 없습니다")
          private String collectionName;

          private String id;
          private Map<String, Object> query;
      }

      /** 문서 삭제 응답 DTO */
      @Data
      @Builder
      @NoArgsConstructor(access = AccessLevel.PROTECTED)
      @AllArgsConstructor(access = AccessLevel.PRIVATE)
      public static class DeleteDocumentResponse {

          private String collectionName;
          private long deletedCount;

          @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
          private LocalDateTime deletedAt;
      }

      /** 컬렉션 통계 응답 DTO */
      @Data
      @Builder
      @NoArgsConstructor(access = AccessLevel.PROTECTED)
      @AllArgsConstructor(access = AccessLevel.PRIVATE)
      public static class CollectionStatsResponse {

          private String collectionName;
          private long documentCount;
          private long dataSize;
          private long storageSize;
          private long indexCount;
          private long indexSize;

          @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
          private LocalDateTime queriedAt;
      }

      /** 데이터베이스 통계 응답 DTO */
      @Data
      @Builder
      @NoArgsConstructor(access = AccessLevel.PROTECTED)
      @AllArgsConstructor(access = AccessLevel.PRIVATE)
      public static class DatabaseStatsResponse {

          private String databaseName;
          private long collectionCount;
          private long documentCount;
          private long dataSize;
          private long storageSize;
          private long indexCount;
          private long indexSize;

          @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
          private LocalDateTime queriedAt;
      }

      /** 서버 상태 응답 DTO */
      @Data
      @Builder
      @NoArgsConstructor(access = AccessLevel.PROTECTED)
      @AllArgsConstructor(access = AccessLevel.PRIVATE)
      public static class ServerStatusResponse {

          private String version;
          private String host;
          private long uptime;
          private Map<String, Object> connections;
          private Map<String, Object> memory;

          @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
          private LocalDateTime queriedAt;
      }
}
