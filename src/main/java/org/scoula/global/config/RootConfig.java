package org.scoula.global.config;

import org.scoula.global.auth.config.SecurityConfig;
import org.scoula.global.email.config.MailConfig;
import org.scoula.global.file.config.S3Config;
import org.scoula.global.mongodb.config.MongoConfig;
import org.scoula.global.oauth2.config.OAuth2ManualConfig;
import org.scoula.global.websocket.config.WebSocketConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

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
      OAuth2ManualConfig.class
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
              "org.scoula.global.file.service",
              "org.scoula.global.oauth2.service",
              "org.scoula.domain.user.service",
          })
public class RootConfig {
      // 각 도메인별 설정은 별도의 Config 클래스로 분리됨
}
