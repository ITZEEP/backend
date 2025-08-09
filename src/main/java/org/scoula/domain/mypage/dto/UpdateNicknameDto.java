package org.scoula.domain.mypage.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

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
@ApiModel(description = "닉네임 업데이트 요청")
public class UpdateNicknameDto {

      @ApiModelProperty(value = "새로운 닉네임", example = "새로운닉네임", required = true)
      @NotBlank(message = "닉네임은 필수입니다.")
      @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하여야 합니다.")
      @Pattern(regexp = "^[가-힣a-zA-Z0-9]+$", message = "닉네임은 한글, 영문, 숫자만 사용 가능합니다.")
      private String nickname;
}
