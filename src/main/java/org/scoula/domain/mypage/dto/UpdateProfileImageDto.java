package org.scoula.domain.mypage.dto;

import javax.validation.constraints.NotNull;

import org.springframework.web.multipart.MultipartFile;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "프로필 이미지 업데이트 요청")
public class UpdateProfileImageDto {
      @ApiModelProperty(value = "프로필 이미지 파일", required = true)
      @NotNull(message = "프로필 이미지는 필수입니다.")
      private MultipartFile profileImage;
}
