package org.scoula.domain.fraud.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskCheckDetailVO {
      // 데이터베이스 실제 컬럼
      private Long riskckId; // 외래키
      private String title1;
      private String title2;
      private String content;
}
