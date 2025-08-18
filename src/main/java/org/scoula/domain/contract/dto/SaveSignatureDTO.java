package org.scoula.domain.contract.dto;

import java.util.List;

import org.scoula.domain.contract.enums.SignedType;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(description = "사인 저장하기")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveSignatureDTO {

      @ApiModelProperty(
              value = "어떤 서명인지 ENUM으로 ",
              example = "OWNER_CONTRACT",
              allowableValues = "TAX, PRIORITY, OWNER_CONTRACT, BUYER_CONTRACT")
      private SignedType signedType;

      @ApiModelProperty(value = "세금 체납 없음 확인", example = "false")
      private Boolean hasTaxArrears;

      @ApiModelProperty(value = "선순위 확정일자 없음 확인", example = "false")
      private Boolean hasPriorFixedDate;

      @ApiModelProperty(value = "조정 동의", example = "true")
      private Boolean mediationAgree;

      @ApiModelProperty(value = "서명 이미지 데이터 (base64)", example = "[\"data:image/png;base64,...\"]")
      private List<String> signatureImages;
}
