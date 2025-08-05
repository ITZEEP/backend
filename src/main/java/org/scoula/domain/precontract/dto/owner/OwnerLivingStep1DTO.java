package org.scoula.domain.precontract.dto.owner;

import org.scoula.domain.precontract.enums.ResponsibilityParty;
import org.scoula.domain.precontract.enums.YesNoEnum;

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
@ApiModel(description = "거주 조건 설정 통합 요청 DTO")
public class OwnerLivingStep1DTO {
      @ApiModelProperty(value = "보증보험 필수 여부", required = true)
      private Boolean requireRentGuaranteeInsurance;

      @ApiModelProperty(value = "보증보험 비용 부담 주체", required = true)
      private ResponsibilityParty insuranceBurden;

      @ApiModelProperty(value = "고지 의무 여부", required = true)
      private YesNoEnum hasNotice;

      @ApiModelProperty(value = "임대인 은행명", required = true)
      private String ownerBankName;

      @ApiModelProperty(value = "임대인 계좌번호", required = true)
      private String ownerAccountNumber;

      // === 월세용 ===
      @ApiModelProperty(value = "납부 예정일 (월세 전용)")
      private Integer paymentDueDate;

      @ApiModelProperty(value = "연체 이자율 (%) (월세 전용)")
      private Double lateFeeInterestRate;
}
