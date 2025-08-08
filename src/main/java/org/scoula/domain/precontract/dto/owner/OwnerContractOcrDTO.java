package org.scoula.domain.precontract.dto.owner;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

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
@ApiModel(description = "특약 저장 요청 DTO")
public class OwnerContractOcrDTO {
      @ApiModelProperty(value = "계약서 특약 정보", required = true)
      @NotNull(message = "계약서 특약 정보는 필수입니다")
      @Valid
      private ContractDocumentDTO contractDocument;

      @ApiModelProperty(value = "계약서 파일 URL", example = "/files/registry/1234567890.pdf")
      private String contractFileUrl;
}
