package org.scoula.global.oauth2.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OAuth2 토큰 응답 DTO
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "OAuth2 토큰 응답", description = "OAuth2 access token 응답 정보")
public class OAuth2TokenResponse {

      @ApiModelProperty(value = "액세스 토큰", example = "ya29.a0AfH6SMC...")
      private String accessToken;

      @ApiModelProperty(value = "토큰 타입", example = "Bearer")
      private String tokenType;

      @ApiModelProperty(value = "리프레시 토큰", example = "1//04...")
      private String refreshToken;

      @ApiModelProperty(value = "만료 시간 (초)", example = "3600")
      private Integer expiresIn;

      @ApiModelProperty(value = "권한 범위", example = "profile_nickname account_email")
      private String scope;
}
