package org.scoula.domain.contract.dto;

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

      //      @ApiModelProperty(value = "사인 이미지")
      //      private MultipartFile signatureImg;

      @ApiModelProperty(
              value = "어떤 서명인지 ENUM으로 ",
              example = "TAX",
              allowableValues = " TAX, PRIORITY, OWNER_CONTRACT, BUYER_CONTRACT")
      private SignedType signedType;
}
