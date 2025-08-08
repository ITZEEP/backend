package org.scoula.domain.health.dto;

import java.time.LocalDateTime;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 헬스체크 응답 DTO */
@ApiModel(description = "헬스체크 응답")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthResponse {

      @ApiModelProperty(value = "서비스 상태", example = "UP", allowableValues = "UP,DOWN")
      private String status;

      @ApiModelProperty(value = "서비스 이름", example = "itzeep-backend")
      private String service;

      @ApiModelProperty(value = "버전 정보", example = "1.0.0")
      private String version;

      @ApiModelProperty(value = "체크 시간", example = "2025-01-20T10:30:00")
      private LocalDateTime timestamp;
}
