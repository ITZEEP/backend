package org.scoula.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import lombok.extern.log4j.Log4j2;

@Configuration
@Log4j2
public class RedisConfig {

      @Value("${redis.host:localhost}")
      private String redisHost;

      @Value("${redis.port:6379}")
      private int redisPort;

      @Value("${redis.password:}")
      private String redisPassword;

      @Value("${redis.database:0}")
      private int redisDatabase;

      @Bean
      public RedisConnectionFactory redisConnectionFactory() {
          RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
          config.setHostName(redisHost);
          config.setPort(redisPort);
          config.setDatabase(redisDatabase);

          if (redisPassword != null && !redisPassword.trim().isEmpty()) {
              config.setPassword(redisPassword);
          }

          JedisConnectionFactory factory = new JedisConnectionFactory(config);

          log.info(
                  "Redis 연결 설정 - Host: {}, Port: {}, Database: {}",
                  redisHost,
                  redisPort,
                  redisDatabase);

          return factory;
      }

      @Bean
      public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
          RedisTemplate<String, Object> template = new RedisTemplate<>();
          template.setConnectionFactory(connectionFactory);

          // Key는 String으로 직렬화
          template.setKeySerializer(new StringRedisSerializer());
          template.setHashKeySerializer(new StringRedisSerializer());

          // Value는 JSON으로 직렬화
          template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
          template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

          template.afterPropertiesSet();

          log.info("Redis Template 설정 완료");

          return template;
      }

      @Bean
      public RedisTemplate<String, String> stringRedisTemplate(
              RedisConnectionFactory connectionFactory) {
          RedisTemplate<String, String> template = new RedisTemplate<>();
          template.setConnectionFactory(connectionFactory);

          // 모든 필드를 String으로 직렬화
          template.setKeySerializer(new StringRedisSerializer());
          template.setValueSerializer(new StringRedisSerializer());
          template.setHashKeySerializer(new StringRedisSerializer());
          template.setHashValueSerializer(new StringRedisSerializer());

          template.afterPropertiesSet();

          return template;
      }
}
