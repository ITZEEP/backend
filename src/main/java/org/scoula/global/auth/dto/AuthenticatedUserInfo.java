package org.scoula.global.auth.dto;

import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 인증된 사용자 정보 DTO
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "인증된 사용자 정보", description = "JWT 토큰으로 인증된 사용자의 정보")
public class AuthenticatedUserInfo {

      @ApiModelProperty(value = "사용자 식별자", example = "user123")
      private String username;

      @ApiModelProperty(value = "사용자 ID", example = "123456789")
      private String userId;

      @ApiModelProperty(value = "이메일", example = "user@example.com")
      private String email;

      @ApiModelProperty(value = "닉네임", example = "홍길동")
      private String nickname;

      @ApiModelProperty(value = "프로필 이미지 URL", example = "http://example.com/profile.jpg")
      private String profileImageUrl;

      @ApiModelProperty(value = "성별", example = "MALE", allowableValues = "MALE,FEMALE")
      private String gender;

      @ApiModelProperty(value = "사용자 권한", example = "[\"ROLE_USER\"]")
      private List<String> authorities;

      @ApiModelProperty(value = "토큰 발급 시간", example = "2024-07-21T10:30:00")
      private String issuedAt;

      @ApiModelProperty(value = "토큰 만료 시간", example = "2024-07-22T10:30:00")
      private String expiresAt;

      @ApiModelProperty(value = "남은 유효 시간 (초)", example = "86400")
      private Long remainingTime;

      @ApiModelProperty(value = "OAuth2 제공자", example = "kakao")
      private String provider;

      @ApiModelProperty(value = "계정 활성화 상태", example = "true")
      private Boolean enabled;

      @ApiModelProperty(value = "계정 잠금 상태", example = "false")
      private Boolean locked;
}
