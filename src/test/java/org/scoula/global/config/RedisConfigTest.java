package org.scoula.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("Redis 설정 단위 테스트")
class RedisConfigTest {

      private RedisConfig redisConfig;

      @BeforeEach
      void setUp() {
          redisConfig = new RedisConfig();
          ReflectionTestUtils.setField(redisConfig, "redisHost", "localhost");
          ReflectionTestUtils.setField(redisConfig, "redisPort", 6379);
          ReflectionTestUtils.setField(redisConfig, "redisPassword", "");
          ReflectionTestUtils.setField(redisConfig, "redisDatabase", 0);
      }

      @Test
      @DisplayName("RedisConnectionFactory Bean이 정상적으로 생성되고 Jedis를 사용하는지 확인")
      void redisConnectionFactory_ShouldBeCreatedAndUseJedis() {
          // when
          RedisConnectionFactory connectionFactory = redisConfig.redisConnectionFactory();

          // then
          assertThat(connectionFactory).isNotNull();
          assertThat(connectionFactory).isInstanceOf(JedisConnectionFactory.class);

          JedisConnectionFactory jedisFactory = (JedisConnectionFactory) connectionFactory;
          assertThat(jedisFactory.getHostName()).isEqualTo("localhost");
          assertThat(jedisFactory.getPort()).isEqualTo(6379);
          assertThat(jedisFactory.getDatabase()).isEqualTo(0);
      }

      @Test
      @DisplayName("RedisTemplate Bean이 정상적으로 생성되고 직렬화 설정이 적용되는지 확인")
      void redisTemplate_ShouldBeCreatedWithProperSerializers() {
          // given
          RedisConnectionFactory connectionFactory = redisConfig.redisConnectionFactory();

          // when
          RedisTemplate<String, Object> redisTemplate = redisConfig.redisTemplate(connectionFactory);

          // then
          assertThat(redisTemplate).isNotNull();
          assertThat(redisTemplate.getConnectionFactory()).isEqualTo(connectionFactory);

          // Key 직렬화 확인
          assertThat(redisTemplate.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
          assertThat(redisTemplate.getHashKeySerializer()).isInstanceOf(StringRedisSerializer.class);

          // Value 직렬화 확인
          assertThat(redisTemplate.getValueSerializer())
                  .isInstanceOf(GenericJackson2JsonRedisSerializer.class);
          assertThat(redisTemplate.getHashValueSerializer())
                  .isInstanceOf(GenericJackson2JsonRedisSerializer.class);
      }

      @Test
      @DisplayName("StringRedisTemplate Bean이 정상적으로 생성되고 String 직렬화 설정이 적용되는지 확인")
      void stringRedisTemplate_ShouldBeCreatedWithStringSerializers() {
          // given
          RedisConnectionFactory connectionFactory = redisConfig.redisConnectionFactory();

          // when
          RedisTemplate<String, String> stringRedisTemplate =
                  redisConfig.stringRedisTemplate(connectionFactory);

          // then
          assertThat(stringRedisTemplate).isNotNull();
          assertThat(stringRedisTemplate.getConnectionFactory()).isEqualTo(connectionFactory);

          // 모든 직렬화가 String으로 설정되어야 함
          assertThat(stringRedisTemplate.getKeySerializer())
                  .isInstanceOf(StringRedisSerializer.class);
          assertThat(stringRedisTemplate.getValueSerializer())
                  .isInstanceOf(StringRedisSerializer.class);
          assertThat(stringRedisTemplate.getHashKeySerializer())
                  .isInstanceOf(StringRedisSerializer.class);
          assertThat(stringRedisTemplate.getHashValueSerializer())
                  .isInstanceOf(StringRedisSerializer.class);
      }
}
