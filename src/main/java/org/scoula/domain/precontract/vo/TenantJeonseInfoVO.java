package org.scoula.domain.precontract.vo;

import org.scoula.domain.precontract.dto.TenantPreContract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantJeonseInfo {

      private Long tenantJeonseRentId; // PK
      private Long tenantPrecheckId; // FK

      private Boolean jeonseLoanPlan;
      private Boolean jeonseInsurancePlan;

      // DTO -> VO
      public static TenantJeonseInfo toVO(TenantPreContract dto) {
            return TenantJeonseInfo.builder()
                    .tenantPrecheckId(dto.getTenantPreCheckId())
                    .jeonseLoanPlan(dto.getLoanPlan())
                    .jeonseInsurancePlan(dto.getInsurancePlan())
                    .build();
      }
}
