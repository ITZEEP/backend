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
public class TenantJeonseInfoVO {

      private Long tenantJeonseRentId; // PK
      private Long tenantPrecheckId; // FK

      private Boolean jeonseLoanPlan;
      private Boolean jeonseInsurancePlan;

      // DTO -> VO
      public static TenantJeonseInfoVO toVO(TenantPreContractDTO dto, Long tenantPrecheckId) {
          return TenantJeonseInfoVO.builder()
                  .tenantPrecheckId(tenantPrecheckId)
                  .jeonseLoanPlan(dto.getLoanPlan())
                  .jeonseInsurancePlan(dto.getInsurancePlan())
                  .build();
      }
}
