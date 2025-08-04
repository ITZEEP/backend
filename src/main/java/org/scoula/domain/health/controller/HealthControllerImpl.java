package org.scoula.domain.health.controller;

import java.time.LocalDateTime;

import org.scoula.domain.health.dto.HealthResponse;
import org.scoula.domain.health.dto.WelcomeResponse;
import org.scoula.domain.health.exception.HomeErrorCode;
import org.scoula.global.common.dto.ApiResponse;
import org.scoula.global.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.log4j.Log4j2;

/** 홈 컨트롤러 구현체 - API 서비스의 기본 엔드포인트와 건강 상태 확인 기능을 구현합니다 */
@RestController
@RequestMapping("/api")
@Log4j2
public class HealthControllerImpl implements HealthController {

      @Value("${spring.application.name:itzeep-backend}")
      private String applicationName;

      @Value("${app.version:1.0.0}")
      private String version;

      /** {@inheritDoc} */
      @Override
      @GetMapping
      public ResponseEntity<ApiResponse<WelcomeResponse>> home() {
          try {
              WelcomeResponse response =
                      WelcomeResponse.builder()
                              .message("ITZeep API에 오신 것을 환영합니다")
                              .applicationName(applicationName)
                              .version(version)
                              .timestamp(LocalDateTime.now())
                              .build();

              return ResponseEntity.ok(ApiResponse.success(response));
          } catch (Exception e) {
              log.error("홈 엔드포인트 처리 중 오류 발생", e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(ApiResponse.error(HomeErrorCode.INTERNAL_ERROR.getMessage()));
          }
      }

      /** {@inheritDoc} */
      @Override
      @GetMapping("/health")
      public ResponseEntity<ApiResponse<HealthResponse>> health() {
          try {
              HealthResponse response =
                      HealthResponse.builder()
                              .status("UP")
                              .service(applicationName)
                              .version(version)
                              .timestamp(LocalDateTime.now())
                              .build();

              return ResponseEntity.ok(ApiResponse.success(response));
          } catch (Exception e) {
              log.error("헬스체크 처리 중 오류 발생", e);
              return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                      .body(ApiResponse.error(HomeErrorCode.SERVICE_UNAVAILABLE.getMessage()));
          }
      }

      /** {@inheritDoc} */
      @Override
      @GetMapping("/test-exception")
      public ResponseEntity<ApiResponse<String>> testException() {
          try {
              // 의도적으로 예외를 발생시킵니다
              if (true) {
                  throw new BusinessException(HomeErrorCode.RATE_LIMIT_EXCEEDED);
              }

              return ResponseEntity.ok(ApiResponse.success("정상 처리되었습니다"));
          } catch (BusinessException e) {
              log.error("비즈니스 예외 발생: {}", e.getMessage());
              return ResponseEntity.status(e.getErrorCode().getHttpStatus())
                      .body(ApiResponse.error(e.getMessage()));
          } catch (Exception e) {
              log.error("예기치 않은 오류 발생", e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(ApiResponse.error(HomeErrorCode.INTERNAL_ERROR.getMessage()));
          }
      }
}
