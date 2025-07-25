package org.scoula.domain.fraud.vo;

import java.time.LocalDateTime;
import java.util.List;

import org.scoula.domain.fraud.enums.RiskType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskCheckVO {
      private Long riskckId; // 데이터베이스 컬럼명과 일치
      private Long userId;
      private Long homeId;
      private RiskType riskType;
      private LocalDateTime checkedAt;
      private String registryFileUrl;
      private String buildingFileUrl;
      private LocalDateTime registryFileDate;
      private LocalDateTime buildingFileDate;

      // 상세 정보 리스트 - JOIN 시 사용
      private List<RiskCheckDetailVO> details;
}
