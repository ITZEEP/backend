package org.scoula.domain.precontract.dto.owner;

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
@ApiModel(description = "거주 조건 설정 요청/응답 DTO (서브 2 - 전세)")
public class OwnerLivingStep2JeonseDTO {

      @ApiModelProperty(value = "전세 정보 ID")
      private Long ownerJeonseRentId;

      @ApiModelProperty(value = "임대인 사전 조사 ID")
      private Long ownerPrecheckId;

      @ApiModelProperty(value = "보증금 조정 가능 여부")
      private Boolean isDepositAdjustable;

      @ApiModelProperty(value = "최소 보증금 조정 가능 금액")
      private Integer depositAdjustmentMin;

      @ApiModelProperty(value = "전세권 설정 허용 여부")
      private Boolean allowJeonseRightRegistration;
}
