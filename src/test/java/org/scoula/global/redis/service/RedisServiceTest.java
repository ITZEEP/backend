package org.scoula.global.redis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
@DisplayName("Redis 서비스 단위 테스트")
class RedisServiceTest {

      @Mock private RedisTemplate<String, Object> redisTemplate;

      @Mock private RedisTemplate<String, String> stringRedisTemplate;

      @Mock private ValueOperations<String, Object> valueOperations;

      @Mock private ValueOperations<String, String> stringValueOperations;

      private RedisServiceInterface redisService;

      @BeforeEach
      void setUp() {
          redisService = new RedisServiceImpl(redisTemplate, stringRedisTemplate);
      }

      @Test
      @DisplayName("값 저장 - 정상적으로 값이 저장됨")
      void setValue_ShouldStoreValueSuccessfully() {
          // given
          String key = "test-key";
          String value = "test-value";

          when(redisTemplate.opsForValue()).thenReturn(valueOperations);

          // when
          redisService.setValue(key, value);

          // then
          verify(valueOperations).set(key, value);
      }

      @Test
      @DisplayName("값 저장 TTL 포함 - 정상적으로 저장됨")
      void setValue_ShouldStoreValueWithTTL() {
          // given
          String key = "test-key";
          String value = "test-value";
          long timeout = 60;
          TimeUnit unit = TimeUnit.SECONDS;

          when(redisTemplate.opsForValue()).thenReturn(valueOperations);

          // when
          redisService.setValue(key, value, timeout, unit);

          // then
          verify(valueOperations).set(key, value, timeout, unit);
      }

      @Test
      @DisplayName("값 저장 Duration 포함 - 정상적으로 저장됨")
      void setValue_ShouldStoreValueWithDuration() {
          // given
          String key = "test-key";
          String value = "test-value";
          Duration timeout = Duration.ofMinutes(5);

          when(redisTemplate.opsForValue()).thenReturn(valueOperations);

          // when
          redisService.setValue(key, value, timeout);

          // then
          verify(valueOperations).set(key, value, timeout);
      }

      @Test
      @DisplayName("값 조회 - 정상적으로 값이 조회됨")
      void getValue_ShouldReturnStoredValue() {
          // given
          String key = "test-key";
          String expectedValue = "test-value";

          when(redisTemplate.opsForValue()).thenReturn(valueOperations);
          when(valueOperations.get(key)).thenReturn(expectedValue);

          // when
          Object result = redisService.getValue(key);

          // then
          assertThat(result).isEqualTo(expectedValue);
      }

      @Test
      @DisplayName("문자열 값 저장 - 정상적으로 저장됨")
      void setStringValue_ShouldStoreStringValue() {
          // given
          String key = "test-key";
          String value = "test-value";

          when(stringRedisTemplate.opsForValue()).thenReturn(stringValueOperations);

          // when
          redisService.setStringValue(key, value);

          // then
          verify(stringValueOperations).set(key, value);
      }

      @Test
      @DisplayName("문자열 값 조회 - 정상적으로 조회됨")
      void getStringValue_ShouldReturnStringValue() {
          // given
          String key = "test-key";
          String expectedValue = "test-value";

          when(stringRedisTemplate.opsForValue()).thenReturn(stringValueOperations);
          when(stringValueOperations.get(key)).thenReturn(expectedValue);

          // when
          String result = redisService.getStringValue(key);

          // then
          assertThat(result).isEqualTo(expectedValue);
      }

      @Test
      @DisplayName("키 삭제 - 정상적으로 삭제됨")
      void deleteValue_ShouldDeleteKey() {
          // given
          String key = "test-key";

          when(redisTemplate.delete(key)).thenReturn(true);

          // when
          boolean result = redisService.deleteValue(key);

          // then
          assertThat(result).isTrue();
          verify(redisTemplate).delete(key);
      }

      @Test
      @DisplayName("키 존재 확인 - 존재하는 키에 대해 true 반환")
      void hasKey_ShouldReturnTrueForExistingKey() {
          // given
          String key = "existing-key";
          when(redisTemplate.hasKey(key)).thenReturn(true);

          // when
          boolean exists = redisService.hasKey(key);

          // then
          assertThat(exists).isTrue();
      }

      @Test
      @DisplayName("키 존재 확인 - 존재하지 않는 키에 대해 false 반환")
      void hasKey_ShouldReturnFalseForNonExistingKey() {
          // given
          String key = "non-existing-key";
          when(redisTemplate.hasKey(key)).thenReturn(false);

          // when
          boolean exists = redisService.hasKey(key);

          // then
          assertThat(exists).isFalse();
      }

      @Test
      @DisplayName("TTL 설정 - 정상적으로 설정됨")
      void expire_ShouldSetTTL() {
          // given
          String key = "test-key";
          long timeout = 60;
          TimeUnit unit = TimeUnit.SECONDS;

          when(redisTemplate.expire(key, timeout, unit)).thenReturn(true);

          // when
          boolean result = redisService.expire(key, timeout, unit);

          // then
          assertThat(result).isTrue();
          verify(redisTemplate).expire(key, timeout, unit);
      }

      @Test
      @DisplayName("TTL 조회 - 정상적으로 조회됨")
      void getExpire_ShouldReturnTTL() {
          // given
          String key = "test-key";
          long expectedTTL = 60L;

          when(redisTemplate.getExpire(key)).thenReturn(expectedTTL);

          // when
          long result = redisService.getExpire(key);

          // then
          assertThat(result).isEqualTo(expectedTTL);
      }

      @Test
      @DisplayName("키 패턴 검색 - 정상적으로 검색됨")
      void getKeys_ShouldReturnMatchingKeys() {
          // given
          String pattern = "test:*";
          Set<String> expectedKeys = Set.of("test:key1", "test:key2");

          when(redisTemplate.keys(pattern)).thenReturn(expectedKeys);

          // when
          Set<String> result = redisService.getKeys(pattern);

          // then
          assertThat(result).isEqualTo(expectedKeys);
      }

      @Test
      @DisplayName("카운터 증가 - 정상적으로 증가됨")
      void increment_ShouldIncrementCounter() {
          // given
          String key = "counter-key";
          long expectedValue = 1L;

          when(redisTemplate.opsForValue()).thenReturn(valueOperations);
          when(valueOperations.increment(key)).thenReturn(expectedValue);

          // when
          long result = redisService.increment(key);

          // then
          assertThat(result).isEqualTo(expectedValue);
      }

      @Test
      @DisplayName("Redis 연결 확인 - 정상 연결 시 true 반환")
      void isConnected_ShouldReturnTrueWhenConnected() {
          // given
          when(redisTemplate.opsForValue()).thenReturn(valueOperations);
          when(valueOperations.get(anyString())).thenReturn(null);

          // when
          boolean isConnected = redisService.isConnected();

          // then
          assertThat(isConnected).isTrue();
      }

      @Test
      @DisplayName("Redis 연결 확인 - 연결 실패 시 false 반환")
      void isConnected_ShouldReturnFalseWhenConnectionFails() {
          // given
          when(redisTemplate.opsForValue()).thenReturn(valueOperations);
          when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Connection failed"));

          // when
          boolean isConnected = redisService.isConnected();

          // then
          assertThat(isConnected).isFalse();
      }
}
