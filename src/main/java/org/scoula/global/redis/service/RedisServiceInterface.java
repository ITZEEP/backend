package org.scoula.global.redis.service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 캐싱 서비스 인터페이스
 *
 * <p>Redis 데이터베이스와의 상호작용을 위한 메서드들을 정의합니다. 키-값 저장, 조회, 삭제 및 TTL 관리 기능을 제공합니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
public interface RedisServiceInterface {

      /**
       * 키-값을 Redis에 저장합니다.
       *
       * @param key 저장할 키
       * @param value 저장할 값
       * @throws RuntimeException Redis 저장 중 오류 발생 시
       */
      void setValue(String key, Object value);

      /**
       * 키-값을 만료시간과 함께 Redis에 저장합니다.
       *
       * @param key 저장할 키
       * @param value 저장할 값
       * @param timeout 만료시간 값
       * @param unit 시간 단위
       * @throws RuntimeException Redis 저장 중 오류 발생 시
       */
      void setValue(String key, Object value, long timeout, TimeUnit unit);

      /**
       * 키-값을 Duration으로 만료시간을 설정하여 Redis에 저장합니다.
       *
       * @param key 저장할 키
       * @param value 저장할 값
       * @param timeout 만료시간 Duration
       * @throws RuntimeException Redis 저장 중 오류 발생 시
       */
      void setValue(String key, Object value, Duration timeout);

      /**
       * 주어진 키의 값을 조회합니다.
       *
       * @param key 조회할 키
       * @return 저장된 값, 없으면 null
       */
      Object getValue(String key);

      /**
       * 문자열 키-값을 Redis에 저장합니다.
       *
       * @param key 저장할 키
       * @param value 저장할 문자열 값
       * @throws RuntimeException Redis 저장 중 오류 발생 시
       */
      void setStringValue(String key, String value);

      /**
       * 문자열 키-값을 만료시간과 함께 Redis에 저장합니다.
       *
       * @param key 저장할 키
       * @param value 저장할 문자열 값
       * @param timeout 만료시간 값
       * @param unit 시간 단위
       * @throws RuntimeException Redis 저장 중 오류 발생 시
       */
      void setStringValue(String key, String value, long timeout, TimeUnit unit);

      /**
       * 주어진 키의 문자열 값을 조회합니다.
       *
       * @param key 조회할 키
       * @return 저장된 문자열 값, 없으면 null
       */
      String getStringValue(String key);

      /**
       * 주어진 키를 삭제합니다.
       *
       * @param key 삭제할 키
       * @return 삭제 성공 여부
       */
      boolean deleteValue(String key);

      /**
       * 주어진 키가 존재하는지 확인합니다.
       *
       * @param key 확인할 키
       * @return 키 존재 여부
       */
      boolean hasKey(String key);

      /**
       * 주어진 키에 만료시간을 설정합니다.
       *
       * @param key 키
       * @param timeout 만료시간 값
       * @param unit 시간 단위
       * @return TTL 설정 성공 여부
       */
      boolean expire(String key, long timeout, TimeUnit unit);

      /**
       * 주어진 키의 남은 만료시간을 조회합니다.
       *
       * @param key 키
       * @return 남은 만료시간(초), 만료시간이 없으면 -1
       */
      long getExpire(String key);

      /**
       * 패턴에 일치하는 키들을 검색합니다.
       *
       * @param pattern 검색 패턴 (예: "user:*")
       * @return 일치하는 키들의 Set
       */
      Set<String> getKeys(String pattern);

      /**
       * 패턴에 일치하는 키들을 모두 삭제합니다.
       *
       * @param pattern 삭제할 키 패턴
       * @return 삭제된 키의 개수
       */
      long deleteKeys(String pattern);

      /**
       * Redis 연결 상태를 확인합니다.
       *
       * @return 연결 상태
       */
      boolean isConnected();

      /**
       * 카운터를 1씩 증가시킵니다 (원자적 연산).
       *
       * @param key 카운터 키
       * @return 증가 후 값
       */
      long increment(String key);

      /**
       * 카운터를 지정된 값만큼 증가시킵니다 (TTL 포함).
       *
       * @param key 카운터 키
       * @param delta 증가시킬 값
       * @param timeout 만료시간 값
       * @param unit 시간 단위
       * @return 증가 후 값
       */
      long incrementBy(String key, long delta, long timeout, TimeUnit unit);
}
