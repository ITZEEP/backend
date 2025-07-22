package org.scoula.global.redis.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scoula.global.common.dto.ApiResponse;
import org.scoula.global.redis.dto.RedisDto;
import org.scoula.global.redis.service.RedisServiceInterface;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("Redis 테스트 컨트롤러 구현체 단위 테스트")
class RedisTestControllerImplTest {

      @Mock private RedisServiceInterface redisService;

      @InjectMocks private RedisTestControllerImpl redisTestController;

      @BeforeEach
      void setUp() {
          ReflectionTestUtils.setField(redisTestController, "redisHost", "localhost");
          ReflectionTestUtils.setField(redisTestController, "redisPort", 6379);
          ReflectionTestUtils.setField(redisTestController, "redisDatabase", 0);
      }

      @Test
      @DisplayName("Redis 헬스체크 - 연결 성공")
      void checkRedisHealth_ShouldReturnUpStatus_WhenConnected() {
          // given
          when(redisService.isConnected()).thenReturn(true);

          // when
          ResponseEntity<ApiResponse<RedisDto.HealthResponse>> response =
                  redisTestController.checkRedisHealth();

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody().isSuccess()).isTrue();
          assertThat(response.getBody().getMessage()).isEqualTo("Redis 연결 정상");

          RedisDto.HealthResponse data = response.getBody().getData();
          assertThat(data.isConnected()).isTrue();
          assertThat(data.getStatus()).isEqualTo("UP");

          RedisDto.ServerInfo serverInfo = data.getServerInfo();
          assertThat(serverInfo.getHost()).isEqualTo("localhost");
          assertThat(serverInfo.getPort()).isEqualTo(6379);
          assertThat(serverInfo.getDatabase()).isEqualTo(0);
          assertThat(serverInfo.getResponseTime()).isGreaterThanOrEqualTo(0);
      }

      @Test
      @DisplayName("Redis 헬스체크 - 연결 실패")
      void checkRedisHealth_ShouldReturnDownStatus_WhenNotConnected() {
          // given
          when(redisService.isConnected()).thenReturn(false);

          // when
          ResponseEntity<ApiResponse<RedisDto.HealthResponse>> response =
                  redisTestController.checkRedisHealth();

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody().isSuccess()).isTrue();
          assertThat(response.getBody().getMessage()).isEqualTo("Redis 연결 실패");

          RedisDto.HealthResponse data = response.getBody().getData();
          assertThat(data.isConnected()).isFalse();
          assertThat(data.getStatus()).isEqualTo("DOWN");

          RedisDto.ServerInfo serverInfo = data.getServerInfo();
          assertThat(serverInfo.getHost()).isEqualTo("localhost");
          assertThat(serverInfo.getPort()).isEqualTo(6379);
          assertThat(serverInfo.getDatabase()).isEqualTo(0);
          assertThat(serverInfo.getResponseTime()).isGreaterThanOrEqualTo(0);
      }

      @Test
      @DisplayName("Redis 헬스체크 - 예외 발생")
      void checkRedisHealth_ShouldReturnErrorStatus_WhenExceptionOccurs() {
          // given
          when(redisService.isConnected()).thenThrow(new RuntimeException("Redis 연결 오류"));

          // when
          ResponseEntity<ApiResponse<RedisDto.HealthResponse>> response =
                  redisTestController.checkRedisHealth();

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody().isSuccess()).isFalse();
          assertThat(response.getBody().getMessage()).contains("Redis 헬스체크 실행 중 오류가 발생했습니다");
      }

      @Test
      @DisplayName("Redis 헬스체크 - 응답 시간 측정")
      void checkRedisHealth_ShouldMeasureResponseTime() throws InterruptedException {
          // given
          when(redisService.isConnected())
                  .thenAnswer(
                          invocation -> {
                              Thread.sleep(10); // 응답 시간 시뮬레이션
                              return true;
                          });

          // when
          ResponseEntity<ApiResponse<RedisDto.HealthResponse>> response =
                  redisTestController.checkRedisHealth();

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
          RedisDto.HealthResponse data = response.getBody().getData();
          assertThat(data.getServerInfo().getResponseTime()).isGreaterThan(0);
      }
}
