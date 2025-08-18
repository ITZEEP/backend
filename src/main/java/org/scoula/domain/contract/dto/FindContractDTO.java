package org.scoula.domain.contract.dto;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(description = "최종 계약서 조회/전송")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindContractDTO {

      private String email; // 계약서를 받을 이메일 주소 (이메일 전송 시 사용)

      @Deprecated private String contractPassword; // 기존 호환성을 위해 유지 (사용하지 않음)
}
