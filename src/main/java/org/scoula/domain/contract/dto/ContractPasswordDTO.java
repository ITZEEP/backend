package org.scoula.domain.contract.dto;

import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(description = "최종 계약서 비밀번호 받아오기")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractPasswordDTO {

      @NotNull private String contractPassword;
      //      private String contractBuyerPassword;

      @NotNull private Boolean mediationAgree; // 동의 여부
}
