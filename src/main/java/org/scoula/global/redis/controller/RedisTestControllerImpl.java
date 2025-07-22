package org.scoula.global.redis.controller;

import org.scoula.global.common.dto.ApiResponse;
import org.scoula.global.redis.dto.RedisDto;
import org.scoula.global.redis.service.RedisServiceInterface;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequestMapping("/api/redis")
@RequiredArgsConstructor
@Log4j2
public class RedisTestControllerImpl implements RedisTestController {

      private final RedisServiceInterface redisService;

      @Value("${redis.host:localhost}")
      private String redisHost;

      @Value("${redis.port:6379}")
      private int redisPort;

      @Value("${redis.database:0}")
      private int redisDatabase;

      /** {@inheritDoc} */
      @Override
      @GetMapping("/health")
      public ResponseEntity<ApiResponse<RedisDto.HealthResponse>> checkRedisHealth() {
          try {
              long startTime = System.currentTimeMillis();
              boolean connected = redisService.isConnected();
              long responseTime = System.currentTimeMillis() - startTime;

              String status = connected ? "UP" : "DOWN";

              RedisDto.ServerInfo serverInfo =
                      RedisDto.ServerInfo.builder()
                              .host(redisHost)
                              .port(redisPort)
                              .database(redisDatabase)
                              .responseTime(responseTime)
                              .build();

              RedisDto.HealthResponse healthResponse =
                      RedisDto.HealthResponse.builder()
                              .connected(connected)
                              .status(status)
                              .serverInfo(serverInfo)
                              .build();

              String message = connected ? "Redis 연결 정상" : "Redis 연결 실패";
              if (!connected) {
                  log.warn(
                          "Redis 연결 실패 - 호스트: {}:{}, 응답시간: {}ms", redisHost, redisPort, responseTime);
              } else {
                  log.debug("Redis 헬스체크 - 상태: {}, 응답시간: {}ms", status, responseTime);
              }

              return ResponseEntity.ok(ApiResponse.success(healthResponse, message));
          } catch (Exception e) {
              log.error("Redis 헬스체크 자체 실행 오류", e);

              RedisDto.HealthResponse errorResponse =
                      RedisDto.HealthResponse.builder().connected(false).status("ERROR").build();

              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(
                              ApiResponse.error(
                                      "REDIS_ERROR",
                                      "Redis 헬스체크 실행 중 오류가 발생했습니다: " + e.getMessage()));
          }
      }
}
