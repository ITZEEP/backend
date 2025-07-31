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
@ApiModel(description = "거주 조건 설정 요청/응답 DTO (서브 1)")
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
      private String ownerBankAccountNumber;
}
