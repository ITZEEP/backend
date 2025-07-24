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
public class TenantWolseInfo {

      private Long tenantWolseRentId; // PK
      private Long tenantPrecheckId; // FK

      private Boolean wolseLoanPlan;
      private Boolean wolseInsurancePlan;

      // DTO -> VO
      public static TenantWolseInfo toVO(TenantPreContract dto) {
          return TenantWolseInfo.builder()
                  .tenantPrecheckId(dto.getTenantPreCheckId())
                  .wolseLoanPlan(dto.getLoanPlan())
                  .wolseInsurancePlan(dto.getInsurancePlan())
                  .build();
      }
}
