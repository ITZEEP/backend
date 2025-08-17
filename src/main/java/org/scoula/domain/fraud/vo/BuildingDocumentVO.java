package org.scoula.domain.fraud.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 건축물대장 문서 정보 VO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildingDocumentVO {
      private Long buildingId;
      private Long riskckId; // risk_check 테이블과 연결

      // 위치 정보
      private String siteLocation;
      private String roadAddress;

      // 건물 정보
      private BigDecimal landArea; // 대지면적
      private BigDecimal totalFloorArea; // 연면적
      private String purpose; // 용도
      private Integer floorNumber; // 층수
      private LocalDate approvalDate; // 사용승인일

      // 위반 여부
      private Boolean isViolationBuilding;

      // 발급일
      private LocalDate issueDate;

      // 메타 정보
      private LocalDateTime createdAt;
      private LocalDateTime updatedAt;
}
