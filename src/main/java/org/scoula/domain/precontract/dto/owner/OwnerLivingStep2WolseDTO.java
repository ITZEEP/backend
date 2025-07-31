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
@ApiModel(description = "거주 조건 설정 요청/응답 DTO (서브 2 - 월세)")
public class OwnerLivingStep2WolseDTO {

      @ApiModelProperty(value = "월세 정보 ID")
      private Long ownerWolseRentId;

      @ApiModelProperty(value = "임대인 사전 조사 ID")
      private Long ownerPrecheckId;

      @ApiModelProperty(value = "보증금 조정 가능 여부")
      private Boolean isAdjustable;

      @ApiModelProperty(value = "월세 조정 가능 여부")
      private Boolean isMonthlyAdjustable;

      @ApiModelProperty(value = "보증금 최소 조정 금액")
      private Integer depositAdjustmentMin;

      @ApiModelProperty(value = "월세 최소 조정 금액")
      private Integer rentAdjustmentMin;

      @ApiModelProperty(value = "납부 예정일")
      private Integer paymentDueDate;

      @ApiModelProperty(value = "연체 이자율 (%)")
      private Double lateFeeInterestRate;
}
