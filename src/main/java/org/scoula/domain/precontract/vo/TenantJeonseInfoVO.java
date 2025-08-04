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
public class TenantJeonseInfoVO {

      private Long tenantJeonseRentId; // PK
      private Long tenantPrecheckId; // FK

      private Boolean jeonseLoanPlan;
      private Boolean jeonseInsurancePlan;

      // DTO -> VO
      public static TenantJeonseInfoVO toVO(TenantStep1DTO step1DTO) {
          return TenantJeonseInfoVO.builder()
                  .jeonseLoanPlan(step1DTO.getLoanPlan())
                  .jeonseInsurancePlan(step1DTO.getInsurancePlan())
                  .build();
      }
}
