package org.scoula.domain.precontract.vo;

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
}
