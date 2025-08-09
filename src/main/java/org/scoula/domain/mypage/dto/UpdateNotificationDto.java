package org.scoula.domain.mypage.dto;

import javax.validation.constraints.NotNull;

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
@ApiModel(description = "알림 설정 업데이트 요청")
public class UpdateNotificationDto {

      @ApiModelProperty(value = "알림 활성화 여부", example = "true", required = true)
      @NotNull(message = "알림 설정 값은 필수입니다.")
      private Boolean notificationEnabled;
}
