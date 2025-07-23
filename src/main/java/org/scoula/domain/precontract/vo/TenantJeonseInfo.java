package org.scoula.domain.precontract.vo;

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
}
