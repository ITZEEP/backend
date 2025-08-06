package org.scoula.domain.mypage.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "마이페이지 사용자 정보")
public class MyPageUserInfoDto {
      @ApiModelProperty(value = "사용자 ID", example = "1")
      private Long userId;

      @ApiModelProperty(value = "이메일 주소", example = "user@example.com")
      private String email;

      @ApiModelProperty(value = "닉네임", example = "집구하는사람")
      private String nickname;

      @ApiModelProperty(value = "프로필 이미지 URL", example = "https://example.com/profile/user1.jpg")
      private String profileImageUrl;

      @ApiModelProperty(value = "알림 활성화 여부", example = "true")
      private Boolean notificationEnabled;
}
