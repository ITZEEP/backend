package org.scoula.global.mongodb.config;

import org.scoula.global.common.util.LogSanitizerUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import lombok.extern.log4j.Log4j2;

/**
 * MongoDB 설정 클래스
 *
 * <p>MongoDB 연결 설정 및 MongoTemplate 빈을 구성합니다. 환경변수를 통해 연결 정보를 설정할 수 있으며, 기본값을 제공합니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@Configuration
@ComponentScan(
          basePackages = {
              "org.scoula.domain.chat.repository",
              "org.scoula.domain.precontract.repository"
          })
@Log4j2
@EnableTransactionManagement
public class MongoConfig extends AbstractMongoClientConfiguration {

      @Value("${mongodb.host:localhost}")
      private String mongoHost;

      @Value("${mongodb.port:27017}")
      private int mongoPort;

      @Value("${mongodb.database:itzeep}")
      private String mongoDatabase;

      @Value("${mongodb.username:}")
      private String mongoUsername;

      @Value("${mongodb.password:}")
      private String mongoPassword;

      @Value("${mongodb.auth-database:admin}")
      private String mongoAuthDatabase;

      @Override
      protected String getDatabaseName() {
          return mongoDatabase;
      }

      @Override
      @Bean
      public MongoClient mongoClient() {
          String connectionString = buildConnectionString();
          log.info(
                  "MongoDB 연결 설정: {}", connectionString.replaceAll("://[^:]*:[^@]*@", "://***:***@"));
          return MongoClients.create(connectionString);
      }

      @Bean
      public MongoTemplate mongoTemplate() {
          MongoTemplate template = new MongoTemplate(mongoClient(), getDatabaseName());

          // _class 필드 제거 설정
          MappingMongoConverter converter = (MappingMongoConverter) template.getConverter();
          converter.setTypeMapper(new DefaultMongoTypeMapper(null));

          log.info(
                  "MongoTemplate 초기화 완료 - Database: {}",
                  LogSanitizerUtil.sanitizeValue(getDatabaseName()));
          return template;
      }

      /**
       * MongoDB 연결 문자열 구성
       *
       * @return MongoDB 연결 URI
       */
      private String buildConnectionString() {
          StringBuilder connectionString = new StringBuilder("mongodb://");

          // 인증 정보가 있는 경우 추가 (사용자명과 비밀번호가 모두 있을 때만)
          if (mongoUsername != null
                  && !mongoUsername.trim().isEmpty()
                  && mongoPassword != null
                  && !mongoPassword.trim().isEmpty()) {
              connectionString.append(mongoUsername);
              connectionString.append(":").append(mongoPassword);
              connectionString.append("@");
          }

          // 호스트:포트
          connectionString.append(mongoHost).append(":").append(mongoPort);

          // 데이터베이스
          connectionString.append("/").append(mongoDatabase);

          // 인증 데이터베이스 설정 (사용자명과 비밀번호가 모두 있을 때만)
          if (mongoUsername != null
                  && !mongoUsername.trim().isEmpty()
                  && mongoPassword != null
                  && !mongoPassword.trim().isEmpty()) {
              connectionString.append("?authSource=").append(mongoAuthDatabase);
          }

          return connectionString.toString();
      }
}
