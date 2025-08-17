package org.scoula.domain.fraud.vo;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 근저당권 정보 VO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MortgageInfoVO {
      private Long mortgageId;
      private Long registryId; // registry_document 테이블과 연결

      private Integer priorityNumber; // 순위번호
      private Long maxClaimAmount; // 채권최고액
      private String debtor; // 채무자
      private String mortgagee; // 근저당권자

      private LocalDateTime createdAt;
}
