package org.scoula.domain.contract.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 서명 제출 DTO - 임대인/임차인 각각의 서명 데이터 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignatureSubmitDTO {

      // 사용자 역할
      private String userRole; // "owner" or "buyer"

      // 서명 데이터 (base64 encoded)
      private String signature1; // 계약서 서명 (필수)
      private String signature2; // 세금 체납 없음 서명 (조건부)
      private String signature3; // 선순위 확정일자 없음 서명 (조건부)

      // 체크박스 상태
      private boolean hasTaxArrears;
      private boolean hasPriorFixedDate;
      private boolean mediationAgree;

      // 타임스탬프
      private Long submittedAt;
}
