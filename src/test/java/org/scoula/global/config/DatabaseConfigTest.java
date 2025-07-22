package org.scoula.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("데이터베이스 설정 단위 테스트")
class DatabaseConfigTest {

      private DatabaseConfig databaseConfig;

      @BeforeEach
      void setUp() {
          ApplicationContext mockContext = mock(ApplicationContext.class);
          databaseConfig = new DatabaseConfig(mockContext);
          ReflectionTestUtils.setField(databaseConfig, "driver", "com.mysql.cj.jdbc.Driver");
          ReflectionTestUtils.setField(databaseConfig, "url", "jdbc:mysql://localhost:3306/testdb");
          ReflectionTestUtils.setField(databaseConfig, "username", "test");
          ReflectionTestUtils.setField(databaseConfig, "password", "test");
      }

      @Test
      @DisplayName("DatabaseConfig Bean이 정상적으로 생성되는지 확인")
      void databaseConfig_ShouldBeCreated() {
          // then
          assertThat(databaseConfig).isNotNull();

          // 필드 값들이 올바르게 설정되었는지 확인
          String driver = (String) ReflectionTestUtils.getField(databaseConfig, "driver");
          String url = (String) ReflectionTestUtils.getField(databaseConfig, "url");
          String username = (String) ReflectionTestUtils.getField(databaseConfig, "username");
          String password = (String) ReflectionTestUtils.getField(databaseConfig, "password");

          assertThat(driver).isEqualTo("com.mysql.cj.jdbc.Driver");
          assertThat(url).isEqualTo("jdbc:mysql://localhost:3306/testdb");
          assertThat(username).isEqualTo("test");
          assertThat(password).isEqualTo("test");
      }

      @Test
      @DisplayName("데이터베이스 설정 클래스가 올바른 어노테이션을 가지고 있는지 확인")
      void databaseConfigClass_ShouldHaveCorrectAnnotations() {
          // when
          Class<DatabaseConfig> configClass = DatabaseConfig.class;

          // then
          assertThat(
                          configClass.isAnnotationPresent(
                                  org.springframework.context.annotation.Configuration.class))
                  .isTrue();
          assertThat(
                          configClass.isAnnotationPresent(
                                  org.springframework.transaction.annotation
                                          .EnableTransactionManagement.class))
                  .isTrue();
      }

      @Test
      @DisplayName("필요한 메서드들이 존재하는지 확인")
      void requiredMethods_ShouldExist() throws NoSuchMethodException {
          // when & then
          assertThat(DatabaseConfig.class.getMethod("dataSource")).isNotNull();
          assertThat(DatabaseConfig.class.getMethod("sqlSessionFactory")).isNotNull();
          assertThat(DatabaseConfig.class.getMethod("transactionManager")).isNotNull();
      }
}
