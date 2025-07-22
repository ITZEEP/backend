package org.scoula.global.mongodb.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bson.Document;
import org.scoula.global.common.service.AbstractExternalService;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * MongoDB 서비스 구현체
 *
 * <p>MongoDB와의 상호작용을 위한 메서드들을 구현합니다. CRUD 연산, 쿼리 실행, 집계 작업 등의 기능을 제공합니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class MongoServiceImpl extends AbstractExternalService implements MongoServiceInterface {

      private final MongoTemplate mongoTemplate;

      /** {@inheritDoc} */
      @Override
      public <T> T save(T document, String collectionName) {
          return executeSafely(
                  () -> {
                      log.debug("문서 저장 - 컬렉션: {}", collectionName);
                      T saved = mongoTemplate.save(document, collectionName);
                      log.debug("문서 저장 완료 - 컬렉션: {}", collectionName);
                      return saved;
                  },
                  "MongoDB 문서 저장");
      }

      /** {@inheritDoc} */
      @Override
      public <T> Optional<T> findById(String id, Class<T> clazz, String collectionName) {
          return executeSafely(
                  () -> {
                      log.debug("ID로 문서 조회 - ID: {}, 컬렉션: {}", id, collectionName);
                      T document = mongoTemplate.findById(id, clazz, collectionName);
                      return Optional.ofNullable(document);
                  },
                  "MongoDB 문서 ID 조회");
      }

      /** {@inheritDoc} */
      @Override
      public <T> List<T> findAll(Class<T> clazz, String collectionName) {
          return executeSafely(
                  () -> {
                      log.debug("모든 문서 조회 - 컬렉션: {}", collectionName);
                      List<T> documents = mongoTemplate.findAll(clazz, collectionName);
                      log.debug("문서 조회 완료 - 개수: {}, 컬렉션: {}", documents.size(), collectionName);
                      return documents;
                  },
                  "MongoDB 전체 문서 조회");
      }

      /** {@inheritDoc} */
      @Override
      public <T> List<T> find(Query query, Class<T> clazz, String collectionName) {
          return executeSafely(
                  () -> {
                      log.debug("쿼리로 문서 조회 - 컬렉션: {}", collectionName);
                      List<T> documents = mongoTemplate.find(query, clazz, collectionName);
                      log.debug("쿼리 조회 완료 - 개수: {}, 컬렉션: {}", documents.size(), collectionName);
                      return documents;
                  },
                  "MongoDB 쿼리 조회");
      }

      /** {@inheritDoc} */
      @Override
      public <T> Optional<T> findOne(Query query, Class<T> clazz, String collectionName) {
          return executeSafely(
                  () -> {
                      log.debug("쿼리로 단일 문서 조회 - 컬렉션: {}", collectionName);
                      T document = mongoTemplate.findOne(query, clazz, collectionName);
                      return Optional.ofNullable(document);
                  },
                  "MongoDB 단일 문서 조회");
      }

      /** {@inheritDoc} */
      @Override
      public long count(Query query, String collectionName) {
          return executeSafely(
                  () -> {
                      log.debug("문서 개수 조회 - 컬렉션: {}", collectionName);
                      long count = mongoTemplate.count(query, collectionName);
                      log.debug("문서 개수: {}, 컬렉션: {}", count, collectionName);
                      return count;
                  },
                  "MongoDB 문서 개수 조회");
      }

      /** {@inheritDoc} */
      @Override
      public <T> T update(T document, String collectionName) {
          return executeSafely(
                  () -> {
                      log.debug("문서 업데이트 - 컬렉션: {}", collectionName);
                      T updated = mongoTemplate.save(document, collectionName);
                      log.debug("문서 업데이트 완료 - 컬렉션: {}", collectionName);
                      return updated;
                  },
                  "MongoDB 문서 업데이트");
      }

      /** {@inheritDoc} */
      @Override
      public boolean deleteById(String id, String collectionName) {
          return executeSafely(
                  () -> {
                      log.debug("ID로 문서 삭제 - ID: {}, 컬렉션: {}", id, collectionName);
                      Query query = new Query(Criteria.where("id").is(id));
                      long deletedCount =
                              mongoTemplate.remove(query, collectionName).getDeletedCount();
                      boolean deleted = deletedCount > 0;
                      log.debug("문서 삭제 결과: {}, 컬렉션: {}", deleted, collectionName);
                      return deleted;
                  },
                  "MongoDB 문서 ID 삭제");
      }

      /** {@inheritDoc} */
      @Override
      public long delete(Query query, String collectionName) {
          return executeSafely(
                  () -> {
                      log.debug("쿼리로 문서 삭제 - 컬렉션: {}", collectionName);
                      long deletedCount =
                              mongoTemplate.remove(query, collectionName).getDeletedCount();
                      log.debug("삭제된 문서 개수: {}, 컬렉션: {}", deletedCount, collectionName);
                      return deletedCount;
                  },
                  "MongoDB 쿼리 삭제");
      }

      /** {@inheritDoc} */
      @Override
      public boolean collectionExists(String collectionName) {
          return executeSafely(
                  () -> {
                      log.debug("컬렉션 존재 여부 확인 - 컬렉션: {}", collectionName);
                      boolean exists = mongoTemplate.collectionExists(collectionName);
                      log.debug("컬렉션 존재 여부: {}, 컬렉션: {}", exists, collectionName);
                      return exists;
                  },
                  "MongoDB 컬렉션 존재 확인");
      }

      /** {@inheritDoc} */
      @Override
      public void createCollection(String collectionName) {
          executeSafely(
                  () -> {
                      log.debug("컬렉션 생성 - 컬렉션: {}", collectionName);
                      if (!mongoTemplate.collectionExists(collectionName)) {
                          mongoTemplate.createCollection(collectionName);
                          log.info("컬렉션 생성 완료 - 컬렉션: {}", collectionName);
                      } else {
                          log.debug("컬렉션이 이미 존재함 - 컬렉션: {}", collectionName);
                      }
                      return null;
                  },
                  "MongoDB 컬렉션 생성");
      }

      /** {@inheritDoc} */
      @Override
      public void dropCollection(String collectionName) {
          executeSafely(
                  () -> {
                      log.debug("컬렉션 삭제 - 컬렉션: {}", collectionName);
                      if (mongoTemplate.collectionExists(collectionName)) {
                          mongoTemplate.dropCollection(collectionName);
                          log.info("컬렉션 삭제 완료 - 컬렉션: {}", collectionName);
                      } else {
                          log.debug("컬렉션이 존재하지 않음 - 컬렉션: {}", collectionName);
                      }
                      return null;
                  },
                  "MongoDB 컬렉션 삭제");
      }

      /** {@inheritDoc} */
      @Override
      public Map<String, Object> getServerStatus() {
          return executeSafely(
                  () -> {
                      log.debug("MongoDB 서버 상태 조회");
                      Document serverStatus =
                              mongoTemplate.getDb().runCommand(new Document("serverStatus", 1));
                      log.debug("서버 상태 조회 완료");
                      return serverStatus;
                  },
                  "MongoDB 서버 상태 조회");
      }

      /** {@inheritDoc} */
      @Override
      public Map<String, Object> getDatabaseStats() {
          return executeSafely(
                  () -> {
                      log.debug("데이터베이스 통계 조회");
                      Document dbStats = mongoTemplate.getDb().runCommand(new Document("dbStats", 1));
                      log.debug("데이터베이스 통계 조회 완료");
                      return dbStats;
                  },
                  "MongoDB 데이터베이스 통계 조회");
      }

      /** {@inheritDoc} */
      @Override
      public Map<String, Object> getCollectionStats(String collectionName) {
          return executeSafely(
                  () -> {
                      log.debug("컬렉션 통계 조회 - 컬렉션: {}", collectionName);
                      Document collStats =
                              mongoTemplate
                                      .getDb()
                                      .runCommand(new Document("collStats", collectionName));
                      log.debug("컬렉션 통계 조회 완료 - 컬렉션: {}", collectionName);
                      return collStats;
                  },
                  "MongoDB 컬렉션 통계 조회");
      }
}
