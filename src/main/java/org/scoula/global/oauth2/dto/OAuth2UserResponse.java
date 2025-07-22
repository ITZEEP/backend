package org.scoula.global.oauth2.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OAuth2 사용자 정보 응답 DTO
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "OAuth2 사용자 정보 응답", description = "OAuth2 사용자 정보")
public class OAuth2UserResponse {

      @ApiModelProperty(value = "사용자 ID", example = "123456789")
      private String id;

      @ApiModelProperty(value = "이메일", example = "user@example.com")
      private String email;

      @ApiModelProperty(value = "닉네임", example = "홍길동")
      private String nickname;

      @ApiModelProperty(value = "프로필 이미지 URL", example = "http://example.com/profile.jpg")
      private String profileImageUrl;

      @ApiModelProperty(value = "성별", example = "male", allowableValues = "male,female")
      private String gender;

      @ApiModelProperty(value = "OAuth2 제공자", example = "kakao")
      private String provider;
}
