package org.scoula.domain.precontract.dto.owner;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

@ApiModel(description = "임대인 사전조사 전체 정보 통합 DTO")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerPreContractDTO {

      @ApiModelProperty(value = "계약 조건 설정 - step1")
      private OwnerContractStep1DTO contractStep1;

      @ApiModelProperty(value = "계약 조건 설정 - step2")
      private OwnerContractStep2DTO contractStep2;

      @ApiModelProperty(value = "거주 조건 설정 - step1")
      private OwnerLivingStep1DTO livingStep1;

      @ApiModelProperty(value = "거주 조건 설정 - step2 - 전세일 경우")
      private OwnerLivingStep2JeonseDTO livingStep2Jeonse;

      @ApiModelProperty(value = "거주 조건 설정 - step2 - 월세일 경우")
      private OwnerLivingStep2WolseDTO livingStep2Wolse;

      @ApiModelProperty(value = "계약서 특약 내용 (OCR 기반)")
      private ContractDocumentDTO contractDocument;
}
