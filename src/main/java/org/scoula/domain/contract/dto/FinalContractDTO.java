package org.scoula.domain.contract.dto;

import org.springframework.web.multipart.MultipartFile;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(description = "최종 계약서")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinalContractDTO {

      private MultipartFile ownerTaxSignature;
      private MultipartFile ownerPrioritySignature;
      private MultipartFile ownerContractSignature;
      private MultipartFile buyerContractSignature;

      private Boolean mediation_agree; // 조정 동의 여부

      private String contractKey; // 계약서 비밀번호
}
