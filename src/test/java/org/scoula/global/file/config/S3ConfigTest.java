package org.scoula.global.file.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import software.amazon.awssdk.services.s3.S3Client;

/**
 * AWS S3 설정 단위 테스트
 *
 * <p>AWS S3 설정 클래스의 Bean 생성 및 설정을 테스트합니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@DisplayName("AWS S3 설정 단위 테스트")
class S3ConfigTest {

      private S3Config s3Config;

      @BeforeEach
      void setUp() {
          s3Config = new S3Config();

          // 테스트용 AWS 설정값 주입
          ReflectionTestUtils.setField(s3Config, "accessKey", "test-access-key");
          ReflectionTestUtils.setField(s3Config, "secretKey", "test-secret-key");
          ReflectionTestUtils.setField(s3Config, "region", "ap-northeast-2");
      }

      @Test
      @DisplayName("S3Client Bean이 정상적으로 생성되는지 확인")
      void s3Client_ShouldBeCreated() {
          // when
          S3Client s3Client = s3Config.s3Client();

          // then
          assertThat(s3Client).isNotNull();

          // S3Client가 제대로 생성되었는지 확인
          s3Client.close();
      }

      @Test
      @DisplayName("다른 리전으로 S3Client가 생성되는지 확인")
      void s3Client_WithDifferentRegion_ShouldBeCreated() {
          // given
          ReflectionTestUtils.setField(s3Config, "region", "us-east-1");

          // when
          S3Client s3Client = s3Config.s3Client();

          // then
          assertThat(s3Client).isNotNull();

          s3Client.close();
      }

      @Test
      @DisplayName("다른 액세스 키로 S3Client가 생성되는지 확인")
      void s3Client_WithDifferentAccessKey_ShouldBeCreated() {
          // given
          ReflectionTestUtils.setField(s3Config, "accessKey", "different-access-key");
          ReflectionTestUtils.setField(s3Config, "secretKey", "different-secret-key");

          // when
          S3Client s3Client = s3Config.s3Client();

          // then
          assertThat(s3Client).isNotNull();

          s3Client.close();
      }

      @Test
      @DisplayName("아시아 태평양 리전으로 S3Client가 생성되는지 확인")
      void s3Client_WithApNortheast2Region_ShouldBeCreated() {
          // given (기본 설정이 이미 ap-northeast-2)

          // when
          S3Client s3Client = s3Config.s3Client();

          // then
          assertThat(s3Client).isNotNull();

          s3Client.close();
      }

      @Test
      @DisplayName("미국 동부 리전으로 S3Client가 생성되는지 확인")
      void s3Client_WithUsEast1Region_ShouldBeCreated() {
          // given
          ReflectionTestUtils.setField(s3Config, "region", "us-east-1");

          // when
          S3Client s3Client = s3Config.s3Client();

          // then
          assertThat(s3Client).isNotNull();

          s3Client.close();
      }

      @Test
      @DisplayName("미국 서부 리전으로 S3Client가 생성되는지 확인")
      void s3Client_WithUsWest2Region_ShouldBeCreated() {
          // given
          ReflectionTestUtils.setField(s3Config, "region", "us-west-2");

          // when
          S3Client s3Client = s3Config.s3Client();

          // then
          assertThat(s3Client).isNotNull();

          s3Client.close();
      }

      @Test
      @DisplayName("유럽 리전으로 S3Client가 생성되는지 확인")
      void s3Client_WithEuWest1Region_ShouldBeCreated() {
          // given
          ReflectionTestUtils.setField(s3Config, "region", "eu-west-1");

          // when
          S3Client s3Client = s3Config.s3Client();

          // then
          assertThat(s3Client).isNotNull();

          s3Client.close();
      }

      @Test
      @DisplayName("빈 문자열 키로 S3Client가 생성되는지 확인")
      void s3Client_WithEmptyKeys_ShouldBeCreated() {
          // given
          ReflectionTestUtils.setField(s3Config, "accessKey", "");
          ReflectionTestUtils.setField(s3Config, "secretKey", "");

          // when
          S3Client s3Client = s3Config.s3Client();

          // then
          assertThat(s3Client).isNotNull();

          s3Client.close();
      }

      @Test
      @DisplayName("프로덕션 설정으로 S3Client가 생성되는지 확인")
      void s3Client_WithProductionSettings_ShouldBeCreated() {
          // given
          ReflectionTestUtils.setField(s3Config, "accessKey", "AKIA1234567890EXAMPLE");
          ReflectionTestUtils.setField(
                  s3Config, "secretKey", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYzEXAMPLEKEY");
          ReflectionTestUtils.setField(s3Config, "region", "ap-northeast-2");

          // when
          S3Client s3Client = s3Config.s3Client();

          // then
          assertThat(s3Client).isNotNull();

          s3Client.close();
      }

      @Test
      @DisplayName("개발 환경 설정으로 S3Client가 생성되는지 확인")
      void s3Client_WithDevelopmentSettings_ShouldBeCreated() {
          // given
          ReflectionTestUtils.setField(s3Config, "accessKey", "dev-access-key");
          ReflectionTestUtils.setField(s3Config, "secretKey", "dev-secret-key");
          ReflectionTestUtils.setField(s3Config, "region", "us-east-1");

          // when
          S3Client s3Client = s3Config.s3Client();

          // then
          assertThat(s3Client).isNotNull();

          s3Client.close();
      }

      @Test
      @DisplayName("여러 번 호출해도 독립적인 S3Client가 생성되는지 확인")
      void s3Client_MultipleCall_ShouldCreateIndependentClients() {
          // when
          S3Client s3Client1 = s3Config.s3Client();
          S3Client s3Client2 = s3Config.s3Client();

          // then
          assertThat(s3Client1).isNotNull();
          assertThat(s3Client2).isNotNull();
          assertThat(s3Client1).isNotSameAs(s3Client2); // 다른 인스턴스여야 함

          s3Client1.close();
          s3Client2.close();
      }
}
