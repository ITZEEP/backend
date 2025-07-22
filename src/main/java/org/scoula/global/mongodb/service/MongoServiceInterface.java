package org.scoula.global.mongodb.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.mongodb.core.query.Query;

/**
 * MongoDB 서비스 인터페이스
 *
 * <p>MongoDB와의 상호작용을 위한 메서드들을 정의합니다. CRUD 연산, 쿼리 실행, 집계 작업 등의 기능을 제공합니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
public interface MongoServiceInterface {

      /**
       * 문서를 저장합니다.
       *
       * @param document 저장할 문서 객체
       * @param collectionName 컬렉션명
       * @param <T> 문서 타입
       * @return 저장된 문서
       */
      <T> T save(T document, String collectionName);

      /**
       * ID로 문서를 조회합니다.
       *
       * @param id 문서 ID
       * @param clazz 문서 클래스
       * @param collectionName 컬렉션명
       * @param <T> 문서 타입
       * @return 조회된 문서 (Optional)
       */
      <T> Optional<T> findById(String id, Class<T> clazz, String collectionName);

      /**
       * 모든 문서를 조회합니다.
       *
       * @param clazz 문서 클래스
       * @param collectionName 컬렉션명
       * @param <T> 문서 타입
       * @return 문서 리스트
       */
      <T> List<T> findAll(Class<T> clazz, String collectionName);

      /**
       * 쿼리를 사용하여 문서를 조회합니다.
       *
       * @param query MongoDB 쿼리
       * @param clazz 문서 클래스
       * @param collectionName 컬렉션명
       * @param <T> 문서 타입
       * @return 문서 리스트
       */
      <T> List<T> find(Query query, Class<T> clazz, String collectionName);

      /**
       * 쿼리를 사용하여 첫 번째 문서를 조회합니다.
       *
       * @param query MongoDB 쿼리
       * @param clazz 문서 클래스
       * @param collectionName 컬렉션명
       * @param <T> 문서 타입
       * @return 조회된 문서 (Optional)
       */
      <T> Optional<T> findOne(Query query, Class<T> clazz, String collectionName);

      /**
       * 문서의 개수를 조회합니다.
       *
       * @param query MongoDB 쿼리
       * @param collectionName 컬렉션명
       * @return 문서 개수
       */
      long count(Query query, String collectionName);

      /**
       * 문서를 업데이트합니다.
       *
       * @param document 업데이트할 문서 객체
       * @param collectionName 컬렉션명
       * @param <T> 문서 타입
       * @return 업데이트된 문서
       */
      <T> T update(T document, String collectionName);

      /**
       * ID로 문서를 삭제합니다.
       *
       * @param id 문서 ID
       * @param collectionName 컬렉션명
       * @return 삭제 성공 여부
       */
      boolean deleteById(String id, String collectionName);

      /**
       * 쿼리를 사용하여 문서를 삭제합니다.
       *
       * @param query MongoDB 쿼리
       * @param collectionName 컬렉션명
       * @return 삭제된 문서 개수
       */
      long delete(Query query, String collectionName);

      /**
       * 컬렉션이 존재하는지 확인합니다.
       *
       * @param collectionName 컬렉션명
       * @return 존재 여부
       */
      boolean collectionExists(String collectionName);

      /**
       * 컬렉션을 생성합니다.
       *
       * @param collectionName 컬렉션명
       */
      void createCollection(String collectionName);

      /**
       * 컬렉션을 삭제합니다.
       *
       * @param collectionName 컬렉션명
       */
      void dropCollection(String collectionName);

      /**
       * MongoDB 서버 상태를 확인합니다.
       *
       * @return 서버 연결 상태 정보
       */
      Map<String, Object> getServerStatus();

      /**
       * 데이터베이스 통계 정보를 조회합니다.
       *
       * @return 데이터베이스 통계
       */
      Map<String, Object> getDatabaseStats();

      /**
       * 컬렉션 통계 정보를 조회합니다.
       *
       * @param collectionName 컬렉션명
       * @return 컬렉션 통계
       */
      Map<String, Object> getCollectionStats(String collectionName);
}
