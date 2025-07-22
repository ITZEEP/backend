package org.scoula.domain.home.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scoula.domain.home.dto.HealthResponse;
import org.scoula.domain.home.dto.WelcomeResponse;
import org.scoula.global.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("홈 컨트롤러 구현체 단위 테스트")
class HomeControllerImplTest {

      private HomeControllerImpl homeController;

      @BeforeEach
      void setUp() {
          homeController = new HomeControllerImpl();

          // 테스트용 설정값 주입
          ReflectionTestUtils.setField(homeController, "applicationName", "itzeep-backend-test");
          ReflectionTestUtils.setField(homeController, "version", "1.0.0-test");
      }

      @Test
      @DisplayName("홈 엔드포인트 - 환영 메시지 반환")
      void home_ShouldReturnWelcomeMessage() {
          // when
          ResponseEntity<ApiResponse<WelcomeResponse>> response = homeController.home();

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody().isSuccess()).isTrue();
          assertThat(response.getBody().getData()).isNotNull();
          assertThat(response.getBody().getData().getMessage()).isEqualTo("ITZeep API에 오신 것을 환영합니다");
          assertThat(response.getBody().getData().getApplicationName())
                  .isEqualTo("itzeep-backend-test");
          assertThat(response.getBody().getData().getVersion()).isEqualTo("1.0.0-test");
          assertThat(response.getBody().getData().getTimestamp()).isNotNull();
      }

      @Test
      @DisplayName("헬스체크 엔드포인트 - UP 상태 반환")
      void health_ShouldReturnUpStatus() {
          // when
          ResponseEntity<ApiResponse<HealthResponse>> response = homeController.health();

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody().isSuccess()).isTrue();
          assertThat(response.getBody().getData()).isNotNull();
          assertThat(response.getBody().getData().getStatus()).isEqualTo("UP");
          assertThat(response.getBody().getData().getService()).isEqualTo("itzeep-backend-test");
          assertThat(response.getBody().getData().getVersion()).isEqualTo("1.0.0-test");
          assertThat(response.getBody().getData().getTimestamp()).isNotNull();
      }

      @Test
      @DisplayName("테스트 예외 엔드포인트 - 예외 발생 시 에러 응답 반환")
      void testException_ShouldReturnErrorResponse() {
          // when
          ResponseEntity<ApiResponse<String>> response = homeController.testException();

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody().isSuccess()).isFalse();
          assertThat(response.getBody().getMessage()).isEqualTo("요청 횟수를 초과했습니다. 잠시 후 다시 시도해주세요.");
      }
}
