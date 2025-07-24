package org.scoula.domain.precontract.vo;

import org.scoula.domain.precontract.dto.TenantPreContractDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantWolseInfoVO {

      private Long tenantWolseRentId; // PK
      private Long tenantPrecheckId; // FK

      private Boolean wolseLoanPlan;
      private Boolean wolseInsurancePlan;

      // DTO -> VO
      public static TenantWolseInfoVO toVO(TenantPreContractDTO dto) {
          return TenantWolseInfoVO.builder()
                  .tenantPrecheckId(dto.getTenantPreCheckId())
                  .wolseLoanPlan(dto.getLoanPlan())
                  .wolseInsurancePlan(dto.getInsurancePlan())
                  .build();
      }
}
