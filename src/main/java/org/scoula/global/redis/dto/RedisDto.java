package org.scoula.global.redis.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class RedisDto {

      @Getter
      @Builder
      @NoArgsConstructor(access = AccessLevel.PROTECTED)
      @AllArgsConstructor(access = AccessLevel.PRIVATE)
      @ApiModel(description = "Redis 헬스체크 응답")
      public static class HealthResponse {
          @ApiModelProperty(value = "연결 상태", example = "true")
          private boolean connected;

          @ApiModelProperty(value = "상태 코드", example = "UP", allowableValues = "UP,DOWN,ERROR")
          private String status;

          @ApiModelProperty(value = "Redis 서버 정보")
          private ServerInfo serverInfo;
      }

      @Getter
      @Builder
      @NoArgsConstructor(access = AccessLevel.PROTECTED)
      @AllArgsConstructor(access = AccessLevel.PRIVATE)
      @ApiModel(description = "Redis 서버 정보")
      public static class ServerInfo {
          @ApiModelProperty(value = "호스트", example = "localhost")
          private String host;

          @ApiModelProperty(value = "포트", example = "6379")
          private int port;

          @ApiModelProperty(value = "데이터베이스 번호", example = "0")
          private int database;

          @ApiModelProperty(value = "응답 시간 (ms)", example = "5")
          private Long responseTime;
      }
}
