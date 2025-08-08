package org.scoula.domain.precontract.vo;

import org.scoula.domain.precontract.dto.tenant.TenantStep1DTO;

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
      public static TenantWolseInfoVO toVO(TenantStep1DTO step1DTO) {
          return TenantWolseInfoVO.builder()
                  .wolseLoanPlan(step1DTO.getLoanPlan())
                  .wolseInsurancePlan(step1DTO.getInsurancePlan())
                  .build();
      }
}
