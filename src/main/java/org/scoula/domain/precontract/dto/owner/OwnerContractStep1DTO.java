package org.scoula.domain.precontract.dto.owner;

import javax.validation.constraints.NotNull;

import org.scoula.domain.precontract.enums.ContractDuration;
import org.scoula.domain.precontract.enums.ResponsibilityParty;
import org.scoula.domain.precontract.enums.YesNoEnum;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@ApiModel(description = "계약 조건 설정 요청/응답 DTO (서브 1)")
public class OwnerContractStep1DTO {
      @ApiModelProperty(value = "근저당 설정 여부", required = true, example = "아니오")
      private Boolean mortgaged;

      @ApiModelProperty(value = "계약 기간", required = true, example = "YEAR_1")
      @NotNull
      private ContractDuration contractDuration;

      @ApiModelProperty(value = "재계약(갱신) 의사", required = true, example = "YES")
      @NotNull
      private YesNoEnum renewalIntent;

      @ApiModelProperty(value = "비품 수리 책임 주체", required = true, example = "OWNER")
      @NotNull
      private ResponsibilityParty responseRepairingFixtures;

      @ApiModelProperty(value = "국세/지방세 미납 여부", required = true)
      @NotNull
      private Boolean hasTaxArrears;

      @ApiModelProperty(value = "국세/지방세 미납 금액", required = true, example = "0")
      @NotNull
      private int taxArrearsAmount;

      @ApiModelProperty(value = "선순위 확정 일자 현황 여부", required = true)
      @NotNull
      private Boolean hasPriorFixedDate;
}
