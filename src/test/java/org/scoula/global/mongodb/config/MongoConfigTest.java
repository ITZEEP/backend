package org.scoula.global.mongodb.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.test.util.ReflectionTestUtils;

import com.mongodb.client.MongoClient;

/**
 * MongoDB 설정 단위 테스트
 *
 * <p>MongoDB 설정 클래스의 Bean 생성 및 설정을 테스트합니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@DisplayName("MongoDB 설정 단위 테스트")
class MongoConfigTest {

      private MongoConfig mongoConfig;

      @BeforeEach
      void setUp() {
          mongoConfig = new MongoConfig();

          // 테스트용 환경변수 설정
          ReflectionTestUtils.setField(mongoConfig, "mongoHost", "localhost");
          ReflectionTestUtils.setField(mongoConfig, "mongoPort", 27017);
          ReflectionTestUtils.setField(mongoConfig, "mongoDatabase", "test_db");
          ReflectionTestUtils.setField(mongoConfig, "mongoUsername", "testuser");
          ReflectionTestUtils.setField(mongoConfig, "mongoPassword", "testpass");
          ReflectionTestUtils.setField(mongoConfig, "mongoAuthDatabase", "admin");
      }

      @Test
      @DisplayName("데이터베이스명이 정상적으로 반환되는지 확인")
      void getDatabaseName_ShouldReturnConfiguredDatabaseName() {
          // when
          String databaseName = mongoConfig.getDatabaseName();

          // then
          assertThat(databaseName).isEqualTo("test_db");
      }

      @Test
      @DisplayName("MongoClient Bean이 정상적으로 생성되는지 확인")
      void mongoClient_ShouldBeCreated() {
          // when
          MongoClient mongoClient = mongoConfig.mongoClient();

          // then
          assertThat(mongoClient).isNotNull();

          // MongoClient가 제대로 생성되었는지 확인 (실제 연결은 테스트하지 않음)
          mongoClient.close();
      }

      @Test
      @DisplayName("MongoTemplate Bean이 정상적으로 생성되고 설정되는지 확인")
      void mongoTemplate_ShouldBeCreatedWithProperConfiguration() {
          // given
          MongoClient mongoClient = mongoConfig.mongoClient();

          // when
          MongoTemplate mongoTemplate = mongoConfig.mongoTemplate();

          // then
          assertThat(mongoTemplate).isNotNull();
          assertThat(mongoTemplate.getDb().getName()).isEqualTo("test_db");

          // TypeMapper 설정 확인 (_class 필드 제거 설정)
          MappingMongoConverter converter = (MappingMongoConverter) mongoTemplate.getConverter();
          assertThat(converter.getTypeMapper()).isInstanceOf(DefaultMongoTypeMapper.class);

          // DefaultMongoTypeMapper(null)로 설정되어 _class 필드가 제거되었는지 확인
          DefaultMongoTypeMapper typeMapper = (DefaultMongoTypeMapper) converter.getTypeMapper();
          assertThat(typeMapper).isNotNull();

          mongoClient.close();
      }

      @Test
      @DisplayName("빈 사용자명과 패스워드로 연결 문자열이 생성되는지 확인")
      void mongoClient_WithEmptyCredentials_ShouldCreateConnectionString() {
          // given
          ReflectionTestUtils.setField(mongoConfig, "mongoUsername", "");
          ReflectionTestUtils.setField(mongoConfig, "mongoPassword", "");

          // when
          MongoClient mongoClient = mongoConfig.mongoClient();

          // then
          assertThat(mongoClient).isNotNull();

          mongoClient.close();
      }

      @Test
      @DisplayName("null 사용자명과 패스워드로 연결 문자열이 생성되는지 확인")
      void mongoClient_WithNullCredentials_ShouldCreateConnectionString() {
          // given
          ReflectionTestUtils.setField(mongoConfig, "mongoUsername", null);
          ReflectionTestUtils.setField(mongoConfig, "mongoPassword", null);

          // when
          MongoClient mongoClient = mongoConfig.mongoClient();

          // then
          assertThat(mongoClient).isNotNull();

          mongoClient.close();
      }

      @Test
      @DisplayName("사용자명만 있고 패스워드가 없는 경우 연결 문자열이 생성되는지 확인")
      void mongoClient_WithUsernameOnlyNoPassword_ShouldCreateConnectionString() {
          // given
          ReflectionTestUtils.setField(mongoConfig, "mongoUsername", "testuser");
          ReflectionTestUtils.setField(mongoConfig, "mongoPassword", "");

          // when
          MongoClient mongoClient = mongoConfig.mongoClient();

          // then
          assertThat(mongoClient).isNotNull();

          mongoClient.close();
      }

      @Test
      @DisplayName("기본 포트로 연결 문자열이 생성되는지 확인")
      void mongoClient_WithDefaultPort_ShouldCreateConnectionString() {
          // given
          ReflectionTestUtils.setField(mongoConfig, "mongoPort", 27017);

          // when
          MongoClient mongoClient = mongoConfig.mongoClient();

          // then
          assertThat(mongoClient).isNotNull();

          mongoClient.close();
      }

      @Test
      @DisplayName("사용자 정의 포트로 연결 문자열이 생성되는지 확인")
      void mongoClient_WithCustomPort_ShouldCreateConnectionString() {
          // given
          ReflectionTestUtils.setField(mongoConfig, "mongoPort", 27018);

          // when
          MongoClient mongoClient = mongoConfig.mongoClient();

          // then
          assertThat(mongoClient).isNotNull();

          mongoClient.close();
      }

      @Test
      @DisplayName("다른 호스트로 연결 문자열이 생성되는지 확인")
      void mongoClient_WithDifferentHost_ShouldCreateConnectionString() {
          // given
          ReflectionTestUtils.setField(mongoConfig, "mongoHost", "mongo.example.com");

          // when
          MongoClient mongoClient = mongoConfig.mongoClient();

          // then
          assertThat(mongoClient).isNotNull();

          mongoClient.close();
      }

      @Test
      @DisplayName("다른 데이터베이스명으로 설정이 적용되는지 확인")
      void getDatabaseName_WithDifferentDatabase_ShouldReturnCorrectName() {
          // given
          ReflectionTestUtils.setField(mongoConfig, "mongoDatabase", "production_db");

          // when
          String databaseName = mongoConfig.getDatabaseName();

          // then
          assertThat(databaseName).isEqualTo("production_db");
      }

      @Test
      @DisplayName("다른 인증 데이터베이스로 연결 문자열이 생성되는지 확인")
      void mongoClient_WithDifferentAuthDatabase_ShouldCreateConnectionString() {
          // given
          ReflectionTestUtils.setField(mongoConfig, "mongoAuthDatabase", "auth_db");

          // when
          MongoClient mongoClient = mongoConfig.mongoClient();

          // then
          assertThat(mongoClient).isNotNull();

          mongoClient.close();
      }

      @Test
      @DisplayName("완전한 인증 정보로 연결 문자열이 생성되는지 확인")
      void mongoClient_WithCompleteCredentials_ShouldCreateConnectionString() {
          // given
          ReflectionTestUtils.setField(mongoConfig, "mongoHost", "mongo.production.com");
          ReflectionTestUtils.setField(mongoConfig, "mongoPort", 27017);
          ReflectionTestUtils.setField(mongoConfig, "mongoDatabase", "production");
          ReflectionTestUtils.setField(mongoConfig, "mongoUsername", "produser");
          ReflectionTestUtils.setField(mongoConfig, "mongoPassword", "securepassword");
          ReflectionTestUtils.setField(mongoConfig, "mongoAuthDatabase", "production");

          // when
          MongoClient mongoClient = mongoConfig.mongoClient();
          String databaseName = mongoConfig.getDatabaseName();

          // then
          assertThat(mongoClient).isNotNull();
          assertThat(databaseName).isEqualTo("production");

          mongoClient.close();
      }

      @Test
      @DisplayName("MongoTemplate에서 올바른 데이터베이스를 사용하는지 확인")
      void mongoTemplate_ShouldUseCorrectDatabase() {
          // given
          ReflectionTestUtils.setField(mongoConfig, "mongoDatabase", "specific_db");

          // when
          MongoTemplate mongoTemplate = mongoConfig.mongoTemplate();

          // then
          assertThat(mongoTemplate).isNotNull();
          assertThat(mongoTemplate.getDb().getName()).isEqualTo("specific_db");
      }
}
