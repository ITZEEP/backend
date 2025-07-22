package org.scoula.global.redis.service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * Redis 캐싱 서비스 구현체
 *
 * <p>Redis 데이터베이스와의 상호작용을 담당하는 서비스입니다. 키-값 저장, 조회, 삭제 및 TTL 관리 기능을 제공합니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class RedisServiceImpl implements RedisServiceInterface {

      private final RedisTemplate<String, Object> redisTemplate;
      private final RedisTemplate<String, String> stringRedisTemplate;

      /** {@inheritDoc} */
      @Override
      public void setValue(String key, Object value) {
          try {
              redisTemplate.opsForValue().set(key, value);
              log.debug("Redis 저장 성공 - Key: {}", key);
          } catch (Exception e) {
              log.error("Redis 저장 실패 - Key: {}", key, e);
              throw new RuntimeException("Redis 저장 중 오류가 발생했습니다.", e);
          }
      }

      /** {@inheritDoc} */
      @Override
      public void setValue(String key, Object value, long timeout, TimeUnit unit) {
          try {
              redisTemplate.opsForValue().set(key, value, timeout, unit);
              log.debug("Redis 저장 성공 (TTL: {}분) - Key: {}", unit.toMinutes(timeout), key);
          } catch (Exception e) {
              log.error("Redis 저장 실패 - Key: {}, TTL: {}분", key, unit.toMinutes(timeout), e);
              throw new RuntimeException("Redis 저장 중 오류가 발생했습니다.", e);
          }
      }

      /** {@inheritDoc} */
      @Override
      public void setValue(String key, Object value, Duration timeout) {
          try {
              redisTemplate.opsForValue().set(key, value, timeout);
              log.debug("Redis 저장 성공 (TTL: {}분) - Key: {}", timeout.toMinutes(), key);
          } catch (Exception e) {
              log.error("Redis 저장 실패 - Key: {}, TTL: {}분", key, timeout.toMinutes(), e);
              throw new RuntimeException("Redis 저장 중 오류가 발생했습니다.", e);
          }
      }

      /** {@inheritDoc} */
      @Override
      public Object getValue(String key) {
          try {
              return redisTemplate.opsForValue().get(key);
          } catch (Exception e) {
              log.error("Redis 조회 실패 - Key: {}", key, e);
              return null;
          }
      }

      /** {@inheritDoc} */
      @Override
      public void setStringValue(String key, String value) {
          try {
              stringRedisTemplate.opsForValue().set(key, value);
              log.debug("Redis String 저장 성공 - Key: {}", key);
          } catch (Exception e) {
              log.error("Redis String 저장 실패 - Key: {}", key, e);
              throw new RuntimeException("Redis 저장 중 오류가 발생했습니다.", e);
          }
      }

      /** {@inheritDoc} */
      @Override
      public void setStringValue(String key, String value, long timeout, TimeUnit unit) {
          try {
              stringRedisTemplate.opsForValue().set(key, value, timeout, unit);
              log.debug("Redis String 저장 성공 (TTL: {}분) - Key: {}", unit.toMinutes(timeout), key);
          } catch (Exception e) {
              log.error("Redis String 저장 실패 - Key: {}, TTL: {}분", key, unit.toMinutes(timeout), e);
              throw new RuntimeException("Redis 저장 중 오류가 발생했습니다.", e);
          }
      }

      /** {@inheritDoc} */
      @Override
      public String getStringValue(String key) {
          try {
              return stringRedisTemplate.opsForValue().get(key);
          } catch (Exception e) {
              log.error("Redis String 조회 실패 - Key: {}", key, e);
              return null;
          }
      }

      /** {@inheritDoc} */
      @Override
      public boolean deleteValue(String key) {
          try {
              Boolean result = redisTemplate.delete(key);
              log.debug("Redis 삭제 - Key: {}, 결과: {}", key, result);
              return result != null && result;
          } catch (Exception e) {
              log.error("Redis 삭제 실패 - Key: {}", key, e);
              return false;
          }
      }

      /** {@inheritDoc} */
      @Override
      public boolean hasKey(String key) {
          try {
              Boolean result = redisTemplate.hasKey(key);
              return result != null && result;
          } catch (Exception e) {
              log.error("Redis 키 존재 확인 실패 - Key: {}", key, e);
              return false;
          }
      }

      /** {@inheritDoc} */
      @Override
      public boolean expire(String key, long timeout, TimeUnit unit) {
          try {
              Boolean result = redisTemplate.expire(key, timeout, unit);
              log.debug("Redis TTL 설정 - Key: {}, TTL: {}분", key, unit.toMinutes(timeout));
              return result != null && result;
          } catch (Exception e) {
              log.error("Redis TTL 설정 실패 - Key: {}", key, e);
              return false;
          }
      }

      /** {@inheritDoc} */
      @Override
      public long getExpire(String key) {
          try {
              Long result = redisTemplate.getExpire(key);
              return result != null ? result : -1;
          } catch (Exception e) {
              log.error("Redis TTL 조회 실패 - Key: {}", key, e);
              return -1;
          }
      }

      /** {@inheritDoc} */
      @Override
      public Set<String> getKeys(String pattern) {
          try {
              return redisTemplate.keys(pattern);
          } catch (Exception e) {
              log.error("Redis 키 검색 실패 - Pattern: {}", pattern, e);
              return Set.of();
          }
      }

      /** {@inheritDoc} */
      @Override
      public long deleteKeys(String pattern) {
          try {
              Set<String> keys = getKeys(pattern);
              if (keys.isEmpty()) {
                  return 0;
              }
              Long result = redisTemplate.delete(keys);
              log.debug("Redis 패턴 삭제 - Pattern: {}, 삭제된 키 수: {}", pattern, result);
              return result != null ? result : 0;
          } catch (Exception e) {
              log.error("Redis 패턴 삭제 실패 - Pattern: {}", pattern, e);
              return 0;
          }
      }

      /** {@inheritDoc} */
      @Override
      public boolean isConnected() {
          try {
              redisTemplate.opsForValue().get("connection_test");
              return true;
          } catch (Exception e) {
              log.warn("Redis 연결 확인 실패", e);
              return false;
          }
      }

      /** {@inheritDoc} */
      @Override
      public long increment(String key) {
          try {
              Long result = redisTemplate.opsForValue().increment(key);
              log.debug("Redis 카운터 증가 - Key: {}, 값: {}", key, result);
              return result != null ? result : 0;
          } catch (Exception e) {
              log.error("Redis 카운터 증가 실패 - Key: {}", key, e);
              return 0;
          }
      }

      /** {@inheritDoc} */
      @Override
      public long incrementBy(String key, long delta, long timeout, TimeUnit unit) {
          try {
              Long result = redisTemplate.opsForValue().increment(key, delta);
              if (result != null && result == delta) { // 첫 생성시에만 TTL 설정
                  expire(key, timeout, unit);
              }
              log.debug("Redis 카운터 증가 - Key: {}, 증가값: {}, 현재값: {}", key, delta, result);
              return result != null ? result : 0;
          } catch (Exception e) {
              log.error("Redis 카운터 증가 실패 - Key: {}, 증가값: {}", key, delta, e);
              return 0;
          }
      }
}
