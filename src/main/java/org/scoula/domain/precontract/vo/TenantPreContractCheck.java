package org.scoula.domain.precontract.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantPreContractCheck {

      private Long tenantPrecheckId; // PK 
      private Long contractChatId; // FK
      private Long identityId; // FK
      private Long riskckId; // FK

      private RiskType riskType;
      private RentType rentType;
      private LocalDate expectedMoveInDate;
      private ContractDuration contractDuration;
      private RenewalIntent renewalIntent;
      private Boolean facilityRepairNeeded;
      private Boolean interiorCleaningNeeded;
      private Boolean applianceInstallationPlan;
      private Boolean hasPet;
      private String petInfo;
      private Long petCount;
      private Boolean indoorSmokingPlan;
      private Boolean earlyTerminationRisk;
      private NonresidentialUsePlan nonresidentialUsePlan;
      private String requestToOwner;
      private LocalDateTime checkedAt;
      private Integer residentCount;
      private String occupation;
      private String emergencyContact;
      private String relation;

      public enum RiskType {
          SAFE,
          WARN,
          DANGER
      }

      public enum RentType {
          JEONSE,
          WOLSE
      }

      public enum ContractDuration {
          YEAR_1,
          YEAR_2,
          YEAR_OVER_2
      }

      public enum RenewalIntent {
          YES,
          NO,
          UNDECIDED
      }

      public enum NonresidentialUsePlan {
          BUSINESS,
          LODGING,
          NONE
      }
}
