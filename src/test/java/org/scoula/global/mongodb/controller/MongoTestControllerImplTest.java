package org.scoula.global.mongodb.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scoula.global.common.dto.ApiResponse;
import org.scoula.global.common.exception.BusinessException;
import org.scoula.global.common.exception.CommonErrorCode;
import org.scoula.global.mongodb.dto.MongoDto;
import org.scoula.global.mongodb.service.MongoServiceInterface;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * MongoTestControllerImpl 단위 테스트
 *
 * <p>MongoDB 테스트 컨트롤러의 기능을 테스트합니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MongoTestControllerImpl 단위 테스트")
class MongoTestControllerImplTest {

      @Mock private MongoServiceInterface mongoService;

      @InjectMocks private MongoTestControllerImpl mongoTestController;

      private final String testCollectionName = "test_collection";

      @Test
      @DisplayName("문서 저장 - 성공")
      void saveDocument_ShouldReturnSuccessResponse() {
          // given
          Map<String, Object> documentData = new HashMap<>();
          documentData.put("title", "테스트 문서");
          documentData.put("content", "테스트 내용");

          MongoDto.SaveDocumentRequest request =
                  MongoDto.SaveDocumentRequest.builder()
                          .collectionName(testCollectionName)
                          .document(documentData)
                          .build();

          Map<String, Object> savedDocument = new HashMap<>(documentData);
          savedDocument.put("_id", "64f5e6b8c9d4e20001234567");

          when(mongoService.save(any(Map.class), eq(testCollectionName))).thenReturn(savedDocument);

          // when
          ResponseEntity<ApiResponse<MongoDto.SaveDocumentResponse>> response =
                  mongoTestController.saveDocument(request);

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody().isSuccess()).isTrue();

          MongoDto.SaveDocumentResponse saveResponse = response.getBody().getData();
          assertThat(saveResponse.getId()).isEqualTo("64f5e6b8c9d4e20001234567");
          assertThat(saveResponse.getCollectionName()).isEqualTo(testCollectionName);
          assertThat(saveResponse.getDocument()).containsEntry("title", "테스트 문서");
          assertThat(saveResponse.getSavedAt()).isNotNull();

          verify(mongoService).save(any(Map.class), eq(testCollectionName));
      }

      @Test
      @DisplayName("문서 조회 - 성공")
      void findDocuments_ShouldReturnDocuments() {
          // given
          Map<String, Object> queryParams = new HashMap<>();
          queryParams.put("status", "active");

          MongoDto.FindDocumentRequest request =
                  MongoDto.FindDocumentRequest.builder()
                          .collectionName(testCollectionName)
                          .query(queryParams)
                          .limit(10)
                          .skip(0)
                          .build();

          Map<String, Object> document1 = new HashMap<>();
          document1.put("_id", "1");
          document1.put("title", "문서1");

          Map<String, Object> document2 = new HashMap<>();
          document2.put("_id", "2");
          document2.put("title", "문서2");

          List<Map> documents = Arrays.asList(document1, document2);

          when(mongoService.find(any(Query.class), eq(Map.class), eq(testCollectionName)))
                  .thenReturn(documents);
          when(mongoService.count(any(Query.class), eq(testCollectionName))).thenReturn(2L);

          // when
          ResponseEntity<ApiResponse<MongoDto.FindDocumentResponse>> response =
                  mongoTestController.findDocuments(request);

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody().isSuccess()).isTrue();

          MongoDto.FindDocumentResponse findResponse = response.getBody().getData();
          assertThat(findResponse.getCollectionName()).isEqualTo(testCollectionName);
          assertThat(findResponse.getTotalCount()).isEqualTo(2L);
          assertThat(findResponse.getDocuments()).hasSize(2);
          assertThat(findResponse.getQueriedAt()).isNotNull();

          verify(mongoService).find(any(Query.class), eq(Map.class), eq(testCollectionName));
          verify(mongoService).count(any(Query.class), eq(testCollectionName));
      }

      @Test
      @DisplayName("ID로 문서 조회 - 성공")
      void findDocumentById_WithExistingDocument_ShouldReturnDocument() {
          // given
          String documentId = "64f5e6b8c9d4e20001234567";
          Map<String, Object> document = new HashMap<>();
          document.put("_id", documentId);
          document.put("title", "테스트 문서");

          when(mongoService.findById(documentId, Map.class, testCollectionName))
                  .thenReturn(Optional.of(document));

          // when
          ResponseEntity<ApiResponse<MongoDto.FindDocumentResponse>> response =
                  mongoTestController.findDocumentById(testCollectionName, documentId);

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody().isSuccess()).isTrue();

          MongoDto.FindDocumentResponse findResponse = response.getBody().getData();
          assertThat(findResponse.getCollectionName()).isEqualTo(testCollectionName);
          assertThat(findResponse.getTotalCount()).isEqualTo(1);
          assertThat(findResponse.getDocuments()).hasSize(1);
          assertThat(findResponse.getDocuments().get(0)).containsEntry("_id", documentId);

          verify(mongoService).findById(documentId, Map.class, testCollectionName);
      }

      @Test
      @DisplayName("ID로 문서 조회 - 존재하지 않는 문서")
      void findDocumentById_WithNonExistentDocument_ShouldReturnEmptyResponse() {
          // given
          String documentId = "nonexistent";

          when(mongoService.findById(documentId, Map.class, testCollectionName))
                  .thenReturn(Optional.empty());

          // when
          ResponseEntity<ApiResponse<MongoDto.FindDocumentResponse>> response =
                  mongoTestController.findDocumentById(testCollectionName, documentId);

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody().isSuccess()).isTrue();

          MongoDto.FindDocumentResponse findResponse = response.getBody().getData();
          assertThat(findResponse.getCollectionName()).isEqualTo(testCollectionName);
          assertThat(findResponse.getTotalCount()).isEqualTo(0);
          assertThat(findResponse.getDocuments()).isEmpty();

          verify(mongoService).findById(documentId, Map.class, testCollectionName);
      }

      @Test
      @DisplayName("문서 업데이트 - 성공")
      void updateDocument_ShouldReturnUpdatedDocument() {
          // given
          String documentId = "64f5e6b8c9d4e20001234567";
          Map<String, Object> documentData = new HashMap<>();
          documentData.put("title", "업데이트된 문서");
          documentData.put("content", "업데이트된 내용");

          MongoDto.UpdateDocumentRequest request =
                  MongoDto.UpdateDocumentRequest.builder()
                          .collectionName(testCollectionName)
                          .id(documentId)
                          .document(documentData)
                          .build();

          Map<String, Object> updatedDocument = new HashMap<>(documentData);
          updatedDocument.put("_id", documentId);

          when(mongoService.update(any(Map.class), eq(testCollectionName)))
                  .thenReturn(updatedDocument);

          // when
          ResponseEntity<ApiResponse<MongoDto.UpdateDocumentResponse>> response =
                  mongoTestController.updateDocument(request);

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody().isSuccess()).isTrue();

          MongoDto.UpdateDocumentResponse updateResponse = response.getBody().getData();
          assertThat(updateResponse.getId()).isEqualTo(documentId);
          assertThat(updateResponse.getCollectionName()).isEqualTo(testCollectionName);
          assertThat(updateResponse.getDocument()).containsEntry("title", "업데이트된 문서");
          assertThat(updateResponse.getUpdatedAt()).isNotNull();

          verify(mongoService).update(any(Map.class), eq(testCollectionName));
      }

      @Test
      @DisplayName("문서 삭제 (쿼리) - 성공")
      void deleteDocuments_WithQuery_ShouldReturnDeletedCount() {
          // given
          Map<String, Object> queryParams = new HashMap<>();
          queryParams.put("status", "inactive");

          MongoDto.DeleteDocumentRequest request =
                  MongoDto.DeleteDocumentRequest.builder()
                          .collectionName(testCollectionName)
                          .query(queryParams)
                          .build();

          when(mongoService.delete(any(Query.class), eq(testCollectionName))).thenReturn(3L);

          // when
          ResponseEntity<ApiResponse<MongoDto.DeleteDocumentResponse>> response =
                  mongoTestController.deleteDocuments(request);

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody().isSuccess()).isTrue();

          MongoDto.DeleteDocumentResponse deleteResponse = response.getBody().getData();
          assertThat(deleteResponse.getCollectionName()).isEqualTo(testCollectionName);
          assertThat(deleteResponse.getDeletedCount()).isEqualTo(3L);
          assertThat(deleteResponse.getDeletedAt()).isNotNull();

          verify(mongoService).delete(any(Query.class), eq(testCollectionName));
      }

      @Test
      @DisplayName("ID로 문서 삭제 - 성공")
      void deleteDocumentById_ShouldReturnDeletedCount() {
          // given
          String documentId = "64f5e6b8c9d4e20001234567";

          when(mongoService.deleteById(documentId, testCollectionName)).thenReturn(true);

          // when
          ResponseEntity<ApiResponse<MongoDto.DeleteDocumentResponse>> response =
                  mongoTestController.deleteDocumentById(testCollectionName, documentId);

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody().isSuccess()).isTrue();

          MongoDto.DeleteDocumentResponse deleteResponse = response.getBody().getData();
          assertThat(deleteResponse.getCollectionName()).isEqualTo(testCollectionName);
          assertThat(deleteResponse.getDeletedCount()).isEqualTo(1L);
          assertThat(deleteResponse.getDeletedAt()).isNotNull();

          verify(mongoService).deleteById(documentId, testCollectionName);
      }

      @Test
      @DisplayName("컬렉션 통계 조회 - 성공")
      void getCollectionStats_ShouldReturnStats() {
          // given
          Map<String, Object> stats = new HashMap<>();
          stats.put("count", 100L);
          stats.put("size", 2048L);
          stats.put("storageSize", 4096L);
          stats.put("nindexes", 2L);
          stats.put("totalIndexSize", 512L);

          when(mongoService.getCollectionStats(testCollectionName)).thenReturn(stats);

          // when
          ResponseEntity<ApiResponse<MongoDto.CollectionStatsResponse>> response =
                  mongoTestController.getCollectionStats(testCollectionName);

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody().isSuccess()).isTrue();

          MongoDto.CollectionStatsResponse statsResponse = response.getBody().getData();
          assertThat(statsResponse.getCollectionName()).isEqualTo(testCollectionName);
          assertThat(statsResponse.getDocumentCount()).isEqualTo(100L);
          assertThat(statsResponse.getDataSize()).isEqualTo(2048L);
          assertThat(statsResponse.getStorageSize()).isEqualTo(4096L);
          assertThat(statsResponse.getIndexCount()).isEqualTo(2L);
          assertThat(statsResponse.getIndexSize()).isEqualTo(512L);
          assertThat(statsResponse.getQueriedAt()).isNotNull();

          verify(mongoService).getCollectionStats(testCollectionName);
      }

      @Test
      @DisplayName("데이터베이스 통계 조회 - 성공")
      void getDatabaseStats_ShouldReturnStats() {
          // given
          Map<String, Object> stats = new HashMap<>();
          stats.put("db", "test_db");
          stats.put("collections", 5L);
          stats.put("objects", 1000L);
          stats.put("dataSize", 10240L);
          stats.put("storageSize", 20480L);
          stats.put("indexes", 10L);
          stats.put("indexSize", 1024L);

          when(mongoService.getDatabaseStats()).thenReturn(stats);

          // when
          ResponseEntity<ApiResponse<MongoDto.DatabaseStatsResponse>> response =
                  mongoTestController.getDatabaseStats();

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody().isSuccess()).isTrue();

          MongoDto.DatabaseStatsResponse statsResponse = response.getBody().getData();
          assertThat(statsResponse.getDatabaseName()).isEqualTo("test_db");
          assertThat(statsResponse.getCollectionCount()).isEqualTo(5L);
          assertThat(statsResponse.getDocumentCount()).isEqualTo(1000L);
          assertThat(statsResponse.getDataSize()).isEqualTo(10240L);
          assertThat(statsResponse.getStorageSize()).isEqualTo(20480L);
          assertThat(statsResponse.getIndexCount()).isEqualTo(10L);
          assertThat(statsResponse.getIndexSize()).isEqualTo(1024L);
          assertThat(statsResponse.getQueriedAt()).isNotNull();

          verify(mongoService).getDatabaseStats();
      }

      @Test
      @DisplayName("서버 상태 조회 - 성공")
      void getServerStatus_ShouldReturnStatus() {
          // given
          Map<String, Object> pingResult = new HashMap<>();
          pingResult.put("ok", 1.0);

          Map<String, Object> status = new HashMap<>();
          status.put("ping", pingResult);
          status.put("database", "test_db");
          status.put("status", "connected");
          status.put("timestamp", new Date());

          when(mongoService.getServerStatus()).thenReturn(status);

          // when
          ResponseEntity<ApiResponse<MongoDto.ServerStatusResponse>> response =
                  mongoTestController.getServerStatus();

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody().isSuccess()).isTrue();

          MongoDto.ServerStatusResponse statusResponse = response.getBody().getData();
          assertThat(statusResponse.getVersion()).isEqualTo("MongoDB Connection");
          assertThat(statusResponse.getHost()).isEqualTo("Connected to test_db");
          assertThat(statusResponse.getUptime()).isEqualTo(0L);
          assertThat(statusResponse.getConnections()).containsKey("ping");
          assertThat(statusResponse.getMemory()).containsKeys("status", "timestamp");
          assertThat(statusResponse.getQueriedAt()).isNotNull();

          verify(mongoService).getServerStatus();
      }

      @Test
      @DisplayName("컬렉션 생성 - 성공")
      void createCollection_ShouldReturnSuccessMessage() {
          // given
          String newCollectionName = "new_collection";
          doNothing().when(mongoService).createCollection(newCollectionName);

          // when
          ResponseEntity<ApiResponse<String>> response =
                  mongoTestController.createCollection(newCollectionName);

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody().isSuccess()).isTrue();
          assertThat(response.getBody().getData()).contains(newCollectionName);

          verify(mongoService).createCollection(newCollectionName);
      }

      @Test
      @DisplayName("컬렉션 삭제 - 성공")
      void dropCollection_ShouldReturnSuccessMessage() {
          // given
          doNothing().when(mongoService).dropCollection(testCollectionName);

          // when
          ResponseEntity<ApiResponse<String>> response =
                  mongoTestController.dropCollection(testCollectionName);

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody().isSuccess()).isTrue();
          assertThat(response.getBody().getData()).contains(testCollectionName);

          verify(mongoService).dropCollection(testCollectionName);
      }

      @Test
      @DisplayName("컬렉션 존재 여부 확인 - 성공")
      void checkCollectionExists_ShouldReturnExistenceStatus() {
          // given
          when(mongoService.collectionExists(testCollectionName)).thenReturn(true);

          // when
          ResponseEntity<ApiResponse<Boolean>> response =
                  mongoTestController.checkCollectionExists(testCollectionName);

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody().isSuccess()).isTrue();
          assertThat(response.getBody().getData()).isTrue();

          verify(mongoService).collectionExists(testCollectionName);
      }

      @Test
      @DisplayName("문서 저장 - 실패 시 예외 처리")
      void saveDocument_WithException_ShouldReturnErrorResponse() {
          // given
          Map<String, Object> documentData = new HashMap<>();
          documentData.put("title", "테스트 문서");

          MongoDto.SaveDocumentRequest request =
                  MongoDto.SaveDocumentRequest.builder()
                          .collectionName(testCollectionName)
                          .document(documentData)
                          .build();

          when(mongoService.save(any(Map.class), eq(testCollectionName)))
                  .thenThrow(
                          new BusinessException(
                                  CommonErrorCode.INTERNAL_SERVER_ERROR, "MongoDB 연결 실패"));

          // when
          ResponseEntity<ApiResponse<MongoDto.SaveDocumentResponse>> response =
                  mongoTestController.saveDocument(request);

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody().isSuccess()).isFalse();
          assertThat(response.getBody().getMessage()).contains("MongoDB 연결 실패");

          verify(mongoService).save(any(Map.class), eq(testCollectionName));
      }
}
