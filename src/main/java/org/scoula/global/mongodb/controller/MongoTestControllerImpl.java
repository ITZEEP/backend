package org.scoula.global.mongodb.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.validation.Valid;

import org.scoula.global.common.dto.ApiResponse;
import org.scoula.global.mongodb.dto.MongoDto;
import org.scoula.global.mongodb.service.MongoServiceInterface;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * MongoDB 테스트 컨트롤러 구현체
 *
 * <p>MongoDB 기능을 테스트하기 위한 API 엔드포인트들을 구현합니다. 문서 저장, 조회, 수정, 삭제 및 통계 조회 기능을 제공합니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/mongo")
@RequiredArgsConstructor
@Log4j2
public class MongoTestControllerImpl implements MongoTestController {

      private final MongoServiceInterface mongoService;

      /** {@inheritDoc} */
      @Override
      @PostMapping("/documents")
      public ResponseEntity<ApiResponse<MongoDto.SaveDocumentResponse>> saveDocument(
              @Valid @RequestBody MongoDto.SaveDocumentRequest request) {
          try {
              // 문서에 ID가 없으면 자동 생성을 위해 추가
              Map<String, Object> document = new HashMap<>(request.getDocument());
              if (!document.containsKey("_id")) {
                  document.put("createdAt", LocalDateTime.now());
              }

              @SuppressWarnings("unchecked")
              Map<String, Object> saved = mongoService.save(document, request.getCollectionName());

              MongoDto.SaveDocumentResponse response =
                      MongoDto.SaveDocumentResponse.builder()
                              .id(saved.get("_id") != null ? saved.get("_id").toString() : null)
                              .collectionName(request.getCollectionName())
                              .document(saved)
                              .savedAt(LocalDateTime.now())
                              .build();

              return ResponseEntity.ok(ApiResponse.success(response, "MongoDB 문서 저장 성공"));
          } catch (Exception e) {
              log.error("MongoDB 문서 저장 실패", e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(ApiResponse.error("MONGO_ERROR", e.getMessage()));
          }
      }

      /** {@inheritDoc} */
      @Override
      @PostMapping("/documents/find")
      public ResponseEntity<ApiResponse<MongoDto.FindDocumentResponse>> findDocuments(
              @Valid @RequestBody MongoDto.FindDocumentRequest request) {
          try {
              Query query = new Query();

              // 쿼리 조건 설정
              if (request.getQuery() != null && !request.getQuery().isEmpty()) {
                  for (Map.Entry<String, Object> entry : request.getQuery().entrySet()) {
                      query.addCriteria(Criteria.where(entry.getKey()).is(entry.getValue()));
                  }
              }

              // 페이징 설정
              if (request.getSkip() != null && request.getSkip() > 0) {
                  query.skip(request.getSkip());
              }
              if (request.getLimit() != null && request.getLimit() > 0) {
                  query.limit(request.getLimit());
              }

              @SuppressWarnings("unchecked")
              List<Map> documents = mongoService.find(query, Map.class, request.getCollectionName());
              long totalCount = mongoService.count(new Query(), request.getCollectionName());

              @SuppressWarnings("unchecked")
              List<Map<String, Object>> documentList =
                      (List<Map<String, Object>>) (List<?>) documents;

              MongoDto.FindDocumentResponse response =
                      MongoDto.FindDocumentResponse.builder()
                              .collectionName(request.getCollectionName())
                              .totalCount(totalCount)
                              .documents(documentList)
                              .queriedAt(LocalDateTime.now())
                              .build();

              return ResponseEntity.ok(ApiResponse.success(response, "MongoDB 문서 조회 성공"));
          } catch (Exception e) {
              log.error("MongoDB 문서 조회 실패", e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(ApiResponse.error("MONGO_ERROR", e.getMessage()));
          }
      }

      /** {@inheritDoc} */
      @Override
      @GetMapping("/documents/{collectionName}/{id}")
      public ResponseEntity<ApiResponse<MongoDto.FindDocumentResponse>> findDocumentById(
              @PathVariable String collectionName, @PathVariable String id) {
          try {
              @SuppressWarnings("unchecked")
              Optional<Map> document = mongoService.findById(id, Map.class, collectionName);

              @SuppressWarnings("unchecked")
              List<Map<String, Object>> documents =
                      document.isPresent()
                              ? List.of((Map<String, Object>) document.get())
                              : List.of();

              MongoDto.FindDocumentResponse response =
                      MongoDto.FindDocumentResponse.builder()
                              .collectionName(collectionName)
                              .totalCount(documents.size())
                              .documents(documents)
                              .queriedAt(LocalDateTime.now())
                              .build();

              return ResponseEntity.ok(ApiResponse.success(response, "MongoDB 문서 ID 조회 성공"));
          } catch (Exception e) {
              log.error("MongoDB 문서 ID 조회 실패", e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(ApiResponse.error("MONGO_ERROR", e.getMessage()));
          }
      }

      /** {@inheritDoc} */
      @Override
      @PutMapping("/documents")
      public ResponseEntity<ApiResponse<MongoDto.UpdateDocumentResponse>> updateDocument(
              @Valid @RequestBody MongoDto.UpdateDocumentRequest request) {
          try {
              // 문서에 업데이트 시간 추가
              Map<String, Object> document = new HashMap<>(request.getDocument());
              document.put("_id", request.getId());
              document.put("updatedAt", LocalDateTime.now());

              @SuppressWarnings("unchecked")
              Map<String, Object> updated =
                      mongoService.update(document, request.getCollectionName());

              MongoDto.UpdateDocumentResponse response =
                      MongoDto.UpdateDocumentResponse.builder()
                              .id(request.getId())
                              .collectionName(request.getCollectionName())
                              .document(updated)
                              .updatedAt(LocalDateTime.now())
                              .build();

              return ResponseEntity.ok(ApiResponse.success(response, "MongoDB 문서 업데이트 성공"));
          } catch (Exception e) {
              log.error("MongoDB 문서 업데이트 실패", e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(ApiResponse.error("MONGO_ERROR", e.getMessage()));
          }
      }

      /** {@inheritDoc} */
      @Override
      @DeleteMapping("/documents")
      public ResponseEntity<ApiResponse<MongoDto.DeleteDocumentResponse>> deleteDocuments(
              @Valid @RequestBody MongoDto.DeleteDocumentRequest request) {
          try {
              long deletedCount;

              if (request.getId() != null) {
                  // ID로 삭제
                  boolean deleted =
                          mongoService.deleteById(request.getId(), request.getCollectionName());
                  deletedCount = deleted ? 1 : 0;
              } else if (request.getQuery() != null && !request.getQuery().isEmpty()) {
                  // 쿼리로 삭제
                  Query query = new Query();
                  for (Map.Entry<String, Object> entry : request.getQuery().entrySet()) {
                      query.addCriteria(Criteria.where(entry.getKey()).is(entry.getValue()));
                  }
                  deletedCount = mongoService.delete(query, request.getCollectionName());
              } else {
                  deletedCount = 0;
              }

              MongoDto.DeleteDocumentResponse response =
                      MongoDto.DeleteDocumentResponse.builder()
                              .collectionName(request.getCollectionName())
                              .deletedCount(deletedCount)
                              .deletedAt(LocalDateTime.now())
                              .build();

              return ResponseEntity.ok(ApiResponse.success(response, "MongoDB 문서 삭제 성공"));
          } catch (Exception e) {
              log.error("MongoDB 문서 삭제 실패", e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(ApiResponse.error("MONGO_ERROR", e.getMessage()));
          }
      }

      /** {@inheritDoc} */
      @Override
      @DeleteMapping("/documents/{collectionName}/{id}")
      public ResponseEntity<ApiResponse<MongoDto.DeleteDocumentResponse>> deleteDocumentById(
              @PathVariable String collectionName, @PathVariable String id) {
          try {
              boolean deleted = mongoService.deleteById(id, collectionName);
              long deletedCount = deleted ? 1 : 0;

              MongoDto.DeleteDocumentResponse response =
                      MongoDto.DeleteDocumentResponse.builder()
                              .collectionName(collectionName)
                              .deletedCount(deletedCount)
                              .deletedAt(LocalDateTime.now())
                              .build();

              return ResponseEntity.ok(ApiResponse.success(response, "MongoDB 문서 ID 삭제 성공"));
          } catch (Exception e) {
              log.error("MongoDB 문서 ID 삭제 실패", e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(ApiResponse.error("MONGO_ERROR", e.getMessage()));
          }
      }

      /** {@inheritDoc} */
      @Override
      @GetMapping("/collections/{collectionName}/stats")
      public ResponseEntity<ApiResponse<MongoDto.CollectionStatsResponse>> getCollectionStats(
              @PathVariable String collectionName) {
          try {
              Map<String, Object> stats = mongoService.getCollectionStats(collectionName);

              MongoDto.CollectionStatsResponse response =
                      MongoDto.CollectionStatsResponse.builder()
                              .collectionName(collectionName)
                              .documentCount(getLongValue(stats, "count"))
                              .dataSize(getLongValue(stats, "size"))
                              .storageSize(getLongValue(stats, "storageSize"))
                              .indexCount(getLongValue(stats, "nindexes"))
                              .indexSize(getLongValue(stats, "totalIndexSize"))
                              .queriedAt(LocalDateTime.now())
                              .build();

              return ResponseEntity.ok(ApiResponse.success(response, "MongoDB 컬렉션 통계 조회 성공"));
          } catch (Exception e) {
              log.error("MongoDB 컬렉션 통계 조회 실패", e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(ApiResponse.error("MONGO_ERROR", e.getMessage()));
          }
      }

      /** {@inheritDoc} */
      @Override
      @GetMapping("/database/stats")
      public ResponseEntity<ApiResponse<MongoDto.DatabaseStatsResponse>> getDatabaseStats() {
          try {
              Map<String, Object> stats = mongoService.getDatabaseStats();

              MongoDto.DatabaseStatsResponse response =
                      MongoDto.DatabaseStatsResponse.builder()
                              .databaseName(getStringValue(stats, "db"))
                              .collectionCount(getLongValue(stats, "collections"))
                              .documentCount(getLongValue(stats, "objects"))
                              .dataSize(getLongValue(stats, "dataSize"))
                              .storageSize(getLongValue(stats, "storageSize"))
                              .indexCount(getLongValue(stats, "indexes"))
                              .indexSize(getLongValue(stats, "indexSize"))
                              .queriedAt(LocalDateTime.now())
                              .build();

              return ResponseEntity.ok(ApiResponse.success(response, "MongoDB 데이터베이스 통계 조회 성공"));
          } catch (Exception e) {
              log.error("MongoDB 데이터베이스 통계 조회 실패", e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(ApiResponse.error("MONGO_ERROR", e.getMessage()));
          }
      }

      /** {@inheritDoc} */
      @Override
      @GetMapping("/server/status")
      public ResponseEntity<ApiResponse<MongoDto.ServerStatusResponse>> getServerStatus() {
          try {
              Map<String, Object> status = mongoService.getServerStatus();

              @SuppressWarnings("unchecked")
              Map<String, Object> connections = (Map<String, Object>) status.get("connections");
              @SuppressWarnings("unchecked")
              Map<String, Object> memory = (Map<String, Object>) status.get("mem");

              MongoDto.ServerStatusResponse response =
                      MongoDto.ServerStatusResponse.builder()
                              .host(getStringValue(status, "host"))
                              .version(getStringValue(status, "version"))
                              .uptime(getLongValue(status, "uptime"))
                              .connections(connections)
                              .memory(memory)
                              .queriedAt(LocalDateTime.now())
                              .build();

              return ResponseEntity.ok(ApiResponse.success(response, "MongoDB 서버 상태 조회 성공"));
          } catch (Exception e) {
              log.error("MongoDB 서버 상태 조회 실패", e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(ApiResponse.error("MONGO_ERROR", e.getMessage()));
          }
      }

      /** {@inheritDoc} */
      @Override
      @PostMapping("/collections/{collectionName}")
      public ResponseEntity<ApiResponse<String>> createCollection(
              @PathVariable String collectionName) {
          try {
              mongoService.createCollection(collectionName);
              return ResponseEntity.ok(
                      ApiResponse.success(
                              "컬렉션 '" + collectionName + "'이 생성되었습니다.", "MongoDB 컬렉션 생성 성공"));
          } catch (Exception e) {
              log.error("MongoDB 컬렉션 생성 실패", e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(ApiResponse.error("MONGO_ERROR", e.getMessage()));
          }
      }

      /** {@inheritDoc} */
      @Override
      @DeleteMapping("/collections/{collectionName}")
      public ResponseEntity<ApiResponse<String>> dropCollection(@PathVariable String collectionName) {
          try {
              mongoService.dropCollection(collectionName);
              return ResponseEntity.ok(
                      ApiResponse.success(
                              "컬렉션 '" + collectionName + "'이 삭제되었습니다.", "MongoDB 컬렉션 삭제 성공"));
          } catch (Exception e) {
              log.error("MongoDB 컬렉션 삭제 실패", e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(ApiResponse.error("MONGO_ERROR", e.getMessage()));
          }
      }

      /** {@inheritDoc} */
      @Override
      @GetMapping("/collections/{collectionName}/exists")
      public ResponseEntity<ApiResponse<Boolean>> checkCollectionExists(
              @PathVariable String collectionName) {
          try {
              boolean exists = mongoService.collectionExists(collectionName);
              return ResponseEntity.ok(ApiResponse.success(exists, "MongoDB 컬렉션 존재 확인 성공"));
          } catch (Exception e) {
              log.error("MongoDB 컬렉션 존재 확인 실패", e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(ApiResponse.error("MONGO_ERROR", e.getMessage()));
          }
      }

      /** Map에서 Long 값을 안전하게 추출 */
      private long getLongValue(Map<String, Object> map, String key) {
          Object value = map.get(key);
          if (value instanceof Number) {
              return ((Number) value).longValue();
          }
          return 0L;
      }

      /** Map에서 String 값을 안전하게 추출 */
      private String getStringValue(Map<String, Object> map, String key) {
          Object value = map.get(key);
          if (value != null) {
              return value.toString();
          }
          return "";
      }
}
