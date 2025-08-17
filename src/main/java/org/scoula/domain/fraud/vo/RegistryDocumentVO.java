package org.scoula.domain.fraud.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 등기부등본 문서 정보 VO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistryDocumentVO {
      private Long registryId;
      private Long riskckId; // risk_check 테이블과 연결

      // 주소 정보
      private String regionAddress;
      private String roadAddress;

      // 건물 정보
      private String buildingNumber;
      private String buildingDetail;

      // 소유자 정보
      private String ownerName;
      private LocalDate ownerBirthDate;
      private String debtor;

      // 권리 상태
      private Boolean hasSeizure;
      private Boolean hasAuction;
      private Boolean hasLitigation;
      private Boolean hasAttachment;

      // 발급일
      private LocalDate issueDate;

      // 메타 정보
      private LocalDateTime createdAt;
      private LocalDateTime updatedAt;
}
