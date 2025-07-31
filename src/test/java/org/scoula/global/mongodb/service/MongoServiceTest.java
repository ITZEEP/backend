package org.scoula.global.mongodb.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import com.mongodb.client.result.DeleteResult;

/**
 * MongoService 단위 테스트
 *
 * <p>MongoDB 서비스의 CRUD 및 관리 기능을 테스트합니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MongoService 단위 테스트")
class MongoServiceTest {

      @Mock private MongoTemplate mongoTemplate;

      private MongoServiceInterface mongoService;

      private final String testCollectionName = "test_collection";

      @BeforeEach
      void setUp() {
          mongoService = new MongoServiceImpl(mongoTemplate);
      }

      @Test
      @DisplayName("문서 저장이 성공적으로 수행되는지 확인")
      void save_WithValidDocument_ShouldReturnSavedDocument() {
          // given
          TestDocument document = new TestDocument("1", "테스트 문서", "테스트 내용");
          when(mongoTemplate.save(document, testCollectionName)).thenReturn(document);

          // when
          TestDocument result = mongoService.save(document, testCollectionName);

          // then
          assertThat(result).isNotNull();
          assertThat(result.getId()).isEqualTo("1");
          assertThat(result.getTitle()).isEqualTo("테스트 문서");
          verify(mongoTemplate).save(document, testCollectionName);
      }

      @Test
      @DisplayName("ID로 문서 조회가 성공적으로 수행되는지 확인")
      void findById_WithValidId_ShouldReturnDocument() {
          // given
          String documentId = "1";
          TestDocument document = new TestDocument(documentId, "테스트 문서", "테스트 내용");
          when(mongoTemplate.findById(documentId, TestDocument.class, testCollectionName))
                  .thenReturn(document);

          // when
          Optional<TestDocument> result =
                  mongoService.findById(documentId, TestDocument.class, testCollectionName);

          // then
          assertThat(result).isPresent();
          assertThat(result.get().getId()).isEqualTo(documentId);
          verify(mongoTemplate).findById(documentId, TestDocument.class, testCollectionName);
      }

      @Test
      @DisplayName("존재하지 않는 ID로 문서 조회 시 빈 Optional을 반환하는지 확인")
      void findById_WithNonExistentId_ShouldReturnEmptyOptional() {
          // given
          String documentId = "nonexistent";
          when(mongoTemplate.findById(documentId, TestDocument.class, testCollectionName))
                  .thenReturn(null);

          // when
          Optional<TestDocument> result =
                  mongoService.findById(documentId, TestDocument.class, testCollectionName);

          // then
          assertThat(result).isEmpty();
          verify(mongoTemplate).findById(documentId, TestDocument.class, testCollectionName);
      }

      @Test
      @DisplayName("모든 문서 조회가 성공적으로 수행되는지 확인")
      void findAll_ShouldReturnAllDocuments() {
          // given
          List<TestDocument> documents =
                  Arrays.asList(
                          new TestDocument("1", "문서1", "내용1"), new TestDocument("2", "문서2", "내용2"));
          when(mongoTemplate.findAll(TestDocument.class, testCollectionName)).thenReturn(documents);

          // when
          List<TestDocument> result = mongoService.findAll(TestDocument.class, testCollectionName);

          // then
          assertThat(result).hasSize(2);
          assertThat(result.get(0).getId()).isEqualTo("1");
          assertThat(result.get(1).getId()).isEqualTo("2");
          verify(mongoTemplate).findAll(TestDocument.class, testCollectionName);
      }

      @Test
      @DisplayName("쿼리로 문서 조회가 성공적으로 수행되는지 확인")
      void find_WithQuery_ShouldReturnMatchingDocuments() {
          // given
          Query query = new Query();
          List<TestDocument> documents = Arrays.asList(new TestDocument("1", "검색된 문서", "검색된 내용"));
          when(mongoTemplate.find(query, TestDocument.class, testCollectionName))
                  .thenReturn(documents);

          // when
          List<TestDocument> result =
                  mongoService.find(query, TestDocument.class, testCollectionName);

          // then
          assertThat(result).hasSize(1);
          assertThat(result.get(0).getTitle()).isEqualTo("검색된 문서");
          verify(mongoTemplate).find(query, TestDocument.class, testCollectionName);
      }

      @Test
      @DisplayName("쿼리로 단일 문서 조회가 성공적으로 수행되는지 확인")
      void findOne_WithQuery_ShouldReturnSingleDocument() {
          // given
          Query query = new Query();
          TestDocument document = new TestDocument("1", "단일 문서", "단일 내용");
          when(mongoTemplate.findOne(query, TestDocument.class, testCollectionName))
                  .thenReturn(document);

          // when
          Optional<TestDocument> result =
                  mongoService.findOne(query, TestDocument.class, testCollectionName);

          // then
          assertThat(result).isPresent();
          assertThat(result.get().getTitle()).isEqualTo("단일 문서");
          verify(mongoTemplate).findOne(query, TestDocument.class, testCollectionName);
      }

      @Test
      @DisplayName("문서 개수 조회가 성공적으로 수행되는지 확인")
      void count_WithQuery_ShouldReturnCount() {
          // given
          Query query = new Query();
          long expectedCount = 5L;
          when(mongoTemplate.count(query, testCollectionName)).thenReturn(expectedCount);

          // when
          long result = mongoService.count(query, testCollectionName);

          // then
          assertThat(result).isEqualTo(expectedCount);
          verify(mongoTemplate).count(query, testCollectionName);
      }

      @Test
      @DisplayName("문서 업데이트가 성공적으로 수행되는지 확인")
      void update_WithValidDocument_ShouldReturnUpdatedDocument() {
          // given
          TestDocument document = new TestDocument("1", "업데이트된 문서", "업데이트된 내용");
          when(mongoTemplate.save(document, testCollectionName)).thenReturn(document);

          // when
          TestDocument result = mongoService.update(document, testCollectionName);

          // then
          assertThat(result).isNotNull();
          assertThat(result.getTitle()).isEqualTo("업데이트된 문서");
          verify(mongoTemplate).save(document, testCollectionName);
      }

      @Test
      @DisplayName("ID로 문서 삭제가 성공적으로 수행되는지 확인")
      void deleteById_WithValidId_ShouldReturnTrue() {
          // given
          String documentId = "1";
          DeleteResult deleteResult = mock(DeleteResult.class);
          when(deleteResult.getDeletedCount()).thenReturn(1L);
          when(mongoTemplate.remove(any(Query.class), eq(testCollectionName)))
                  .thenReturn(deleteResult);

          // when
          boolean result = mongoService.deleteById(documentId, testCollectionName);

          // then
          assertThat(result).isTrue();
          verify(mongoTemplate).remove(any(Query.class), eq(testCollectionName));
      }

      @Test
      @DisplayName("존재하지 않는 ID로 문서 삭제 시 false를 반환하는지 확인")
      void deleteById_WithNonExistentId_ShouldReturnFalse() {
          // given
          String documentId = "nonexistent";
          DeleteResult deleteResult = mock(DeleteResult.class);
          when(deleteResult.getDeletedCount()).thenReturn(0L);
          when(mongoTemplate.remove(any(Query.class), eq(testCollectionName)))
                  .thenReturn(deleteResult);

          // when
          boolean result = mongoService.deleteById(documentId, testCollectionName);

          // then
          assertThat(result).isFalse();
          verify(mongoTemplate).remove(any(Query.class), eq(testCollectionName));
      }

      @Test
      @DisplayName("쿼리로 문서 삭제가 성공적으로 수행되는지 확인")
      void delete_WithQuery_ShouldReturnDeletedCount() {
          // given
          Query query = new Query();
          long expectedDeletedCount = 3L;
          DeleteResult deleteResult = mock(DeleteResult.class);
          when(deleteResult.getDeletedCount()).thenReturn(expectedDeletedCount);
          when(mongoTemplate.remove(query, testCollectionName)).thenReturn(deleteResult);

          // when
          long result = mongoService.delete(query, testCollectionName);

          // then
          assertThat(result).isEqualTo(expectedDeletedCount);
          verify(mongoTemplate).remove(query, testCollectionName);
      }

      @Test
      @DisplayName("컬렉션 존재 여부 확인이 성공적으로 수행되는지 확인")
      void collectionExists_WithExistingCollection_ShouldReturnTrue() {
          // given
          when(mongoTemplate.collectionExists(testCollectionName)).thenReturn(true);

          // when
          boolean result = mongoService.collectionExists(testCollectionName);

          // then
          assertThat(result).isTrue();
          verify(mongoTemplate).collectionExists(testCollectionName);
      }

      @Test
      @DisplayName("존재하지 않는 컬렉션 확인 시 false를 반환하는지 확인")
      void collectionExists_WithNonExistentCollection_ShouldReturnFalse() {
          // given
          String nonExistentCollection = "nonexistent_collection";
          when(mongoTemplate.collectionExists(nonExistentCollection)).thenReturn(false);

          // when
          boolean result = mongoService.collectionExists(nonExistentCollection);

          // then
          assertThat(result).isFalse();
          verify(mongoTemplate).collectionExists(nonExistentCollection);
      }

      @Test
      @DisplayName("컬렉션 생성이 성공적으로 수행되는지 확인")
      void createCollection_WithNewCollection_ShouldCreateCollection() {
          // given
          String newCollectionName = "new_collection";
          when(mongoTemplate.collectionExists(newCollectionName)).thenReturn(false);

          // when
          mongoService.createCollection(newCollectionName);

          // then
          verify(mongoTemplate).collectionExists(newCollectionName);
          verify(mongoTemplate).createCollection(newCollectionName);
      }

      @Test
      @DisplayName("이미 존재하는 컬렉션 생성 시 새로 생성하지 않는지 확인")
      void createCollection_WithExistingCollection_ShouldNotCreateAgain() {
          // given
          when(mongoTemplate.collectionExists(testCollectionName)).thenReturn(true);

          // when
          mongoService.createCollection(testCollectionName);

          // then
          verify(mongoTemplate).collectionExists(testCollectionName);
          verify(mongoTemplate, org.mockito.Mockito.never()).createCollection(testCollectionName);
      }

      @Test
      @DisplayName("컬렉션 삭제가 성공적으로 수행되는지 확인")
      void dropCollection_WithExistingCollection_ShouldDropCollection() {
          // given
          when(mongoTemplate.collectionExists(testCollectionName)).thenReturn(true);

          // when
          mongoService.dropCollection(testCollectionName);

          // then
          verify(mongoTemplate).collectionExists(testCollectionName);
          verify(mongoTemplate).dropCollection(testCollectionName);
      }

      @Test
      @DisplayName("존재하지 않는 컬렉션 삭제 시 삭제하지 않는지 확인")
      void dropCollection_WithNonExistentCollection_ShouldNotDrop() {
          // given
          String nonExistentCollection = "nonexistent_collection";
          when(mongoTemplate.collectionExists(nonExistentCollection)).thenReturn(false);

          // when
          mongoService.dropCollection(nonExistentCollection);

          // then
          verify(mongoTemplate).collectionExists(nonExistentCollection);
          verify(mongoTemplate, org.mockito.Mockito.never()).dropCollection(nonExistentCollection);
      }

      @Test
      @DisplayName("서버 상태 조회가 성공적으로 수행되는지 확인")
      void getServerStatus_ShouldReturnServerStatus() {
          // given
          Document pingResult = new Document("ok", 1.0);
          com.mongodb.client.MongoDatabase mockDatabase =
                  mock(com.mongodb.client.MongoDatabase.class);
          when(mongoTemplate.getDb()).thenReturn(mockDatabase);
          when(mockDatabase.runCommand(any(Document.class))).thenReturn(pingResult);
          when(mockDatabase.getName()).thenReturn("test_db");

          // when
          Map<String, Object> result = mongoService.getServerStatus();

          // then
          assertThat(result).isNotNull();
          assertThat(result.get("ping")).isEqualTo(pingResult);
          assertThat(result.get("database")).isEqualTo("test_db");
          assertThat(result.get("status")).isEqualTo("connected");
          assertThat(result.get("timestamp")).isNotNull();
          verify(mockDatabase).runCommand(any(Document.class));
      }

      @Test
      @DisplayName("데이터베이스 통계 조회가 성공적으로 수행되는지 확인")
      void getDatabaseStats_ShouldReturnDatabaseStats() {
          // given
          Document dbStats = new Document("dataSize", 1024).append("collections", 5);
          com.mongodb.client.MongoDatabase mockDatabase =
                  mock(com.mongodb.client.MongoDatabase.class);
          when(mongoTemplate.getDb()).thenReturn(mockDatabase);
          when(mockDatabase.runCommand(any(Document.class))).thenReturn(dbStats);

          // when
          Map<String, Object> result = mongoService.getDatabaseStats();

          // then
          assertThat(result).isNotNull();
          assertThat(result.get("dataSize")).isEqualTo(1024);
          assertThat(result.get("collections")).isEqualTo(5);
          verify(mockDatabase).runCommand(any(Document.class));
      }

      @Test
      @DisplayName("컬렉션 통계 조회가 성공적으로 수행되는지 확인")
      void getCollectionStats_ShouldReturnCollectionStats() {
          // given
          Document collStats = new Document("count", 100).append("size", 2048);
          com.mongodb.client.MongoDatabase mockDatabase =
                  mock(com.mongodb.client.MongoDatabase.class);
          when(mongoTemplate.getDb()).thenReturn(mockDatabase);
          when(mockDatabase.runCommand(any(Document.class))).thenReturn(collStats);

          // when
          Map<String, Object> result = mongoService.getCollectionStats(testCollectionName);

          // then
          assertThat(result).isNotNull();
          assertThat(result.get("count")).isEqualTo(100);
          assertThat(result.get("size")).isEqualTo(2048);
          verify(mockDatabase).runCommand(any(Document.class));
      }

      // 테스트용 문서 클래스
      private static class TestDocument {
          private String id;
          private String title;
          private String content;

          public TestDocument() {}

          public TestDocument(String id, String title, String content) {
              this.id = id;
              this.title = title;
              this.content = content;
          }

          public String getId() {
              return id;
          }

          public void setId(String id) {
              this.id = id;
          }

          public String getTitle() {
              return title;
          }

          public void setTitle(String title) {
              this.title = title;
          }

          public String getContent() {
              return content;
          }

          public void setContent(String content) {
              this.content = content;
          }
      }
}
