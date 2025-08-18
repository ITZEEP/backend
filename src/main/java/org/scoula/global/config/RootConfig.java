package org.scoula.global.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.scoula.global.auth.config.SecurityConfig;
import org.scoula.global.email.config.MailConfig;
import org.scoula.global.file.config.S3Config;
import org.scoula.global.mongodb.config.MongoConfig;
import org.scoula.global.oauth2.config.OAuth2ManualConfig;
import org.scoula.global.websocket.config.WebSocketConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

/** 루트 설정 클래스 - 전체 애플리케이션의 공통 설정을 관리합니다 */
@Configuration
@PropertySource({
      "classpath:application.properties",
      "classpath:application-${spring.profiles.active:default}.properties"
})
@Import({
      DatabaseConfig.class,
      MailConfig.class,
      S3Config.class,
      MongoConfig.class,
      SecurityConfig.class,
      WebSocketConfig.class,
      RedisConfig.class,
      OAuth2ManualConfig.class,
      AsyncConfig.class
})
@ComponentScan(
          basePackages = {
              "org.scoula.global.auth.service",
              "org.scoula.global.auth.jwt",
              "org.scoula.global.auth.handler",
              "org.scoula.global.auth.filter",
              "org.scoula.global.email.service",
              "org.scoula.global.redis.service",
              "org.scoula.global.mongodb.service",
              "org.scoula.global.common.aop",
              "org.scoula.global.common.service",
              "org.scoula.global.common.util",
              "org.scoula.global.file.service",
              "org.scoula.global.oauth2.service",
              "org.scoula.domain.user.service",
              "org.scoula.domain.chat",
              "org.scoula.domain.fraud.service",
              "org.scoula.domain.precontract.service",
              "org.scoula.domain.verification.service",
              "org.scoula.domain.home.service",
              "org.scoula.domain.mypage.service",
              "org.scoula.domain.contract.service"
          })
public class RootConfig {
      // 각 도메인별 설정은 별도의 Config 클래스로 분리됨

      @Bean
      public ObjectMapper objectMapper() {
          ObjectMapper objectMapper = new ObjectMapper();
          objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
          objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
          objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

          // JavaTimeModule with custom date formats
          JavaTimeModule javaTimeModule = new JavaTimeModule();

          // LocalDate formatter (yyyy-MM-dd)
          DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
          javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
          javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));

          // LocalDateTime formatter (yyyy-MM-dd HH:mm:ss)
          DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
          javaTimeModule.addSerializer(
                  LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
          javaTimeModule.addDeserializer(
                  LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));

          objectMapper.registerModule(javaTimeModule);
          return objectMapper;
      }
}
