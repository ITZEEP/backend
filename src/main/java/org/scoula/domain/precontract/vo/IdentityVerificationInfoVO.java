package org.scoula.domain.precontract.vo;

import java.time.LocalDateTime;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class IdentityVerificationInfoVO {

      private Long identityId;

      private Long userId;
      private Long contractId;

      private String name; // 이름
      private String ssnFront; // 주민등록번호 앞자리
      private String ssnBack; // 주민등록번호 뒷자리

      private LocalDateTime identityVerifiedAt; // 본인 인증 완료 시간

      private ContractStep contractStep; // 계약 단계 enum

      private String addr1; // 주소 1
      private String addr2; // 주소 2
      private String phoneNumber; // 전화번호

      public enum ContractStep {
          START,
          END
      }
}
