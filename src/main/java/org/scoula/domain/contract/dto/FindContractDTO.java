package org.scoula.domain.contract.dto;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(description = "최종 계약서 비밀번호 받기")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindContractDTO {

      private String contractPassword;
}
