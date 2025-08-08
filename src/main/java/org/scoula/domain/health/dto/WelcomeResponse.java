package org.scoula.domain.health.dto;

import java.time.LocalDateTime;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 환영 메시지 응답 DTO */
@ApiModel(description = "환영 메시지 응답")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WelcomeResponse {

      @ApiModelProperty(value = "환영 메시지", example = "ITZeep API에 오신 것을 환영합니다")
      private String message;

      @ApiModelProperty(value = "애플리케이션 이름", example = "itzeep-backend")
      private String applicationName;

      @ApiModelProperty(value = "버전 정보", example = "1.0.0")
      private String version;

      @ApiModelProperty(value = "응답 시간", example = "2025-01-20T10:30:00")
      private LocalDateTime timestamp;
}
