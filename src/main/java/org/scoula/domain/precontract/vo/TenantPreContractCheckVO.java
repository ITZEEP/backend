package org.scoula.domain.precontract.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.scoula.domain.precontract.dto.TenantPreContractDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantPreContractCheckVO {

      private Long tenantPrecheckId; // PK -> column명 바꿔도 되는지 물어보기 -> erd(노션)도 바꾸기
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

      // DTO -> VO
      public static TenantPreContractCheckVO toVO(TenantPreContractDTO dto) {
          return TenantPreContractCheckVO.builder()
                  .tenantPrecheckId(dto.getTenantPreCheckId())
                  .contractChatId(dto.getContractChatId())
                  .identityId(dto.getIdentityId())
                  .riskckId(dto.getRiskckId())
                  .riskType(RiskType.valueOf(dto.getRiskType()))
                  .rentType(RentType.valueOf(dto.getRentType()))
                  .expectedMoveInDate(dto.getExpectedMoveInDate())
                  .contractDuration(ContractDuration.valueOf(dto.getContractDuration()))
                  .renewalIntent(RenewalIntent.valueOf(dto.getRenewalIntent()))
                  .facilityRepairNeeded(dto.getFacilityRepairNeeded())
                  .interiorCleaningNeeded(dto.getInteriorCleaningNeeded())
                  .applianceInstallationPlan(dto.getApplianceInstallationPlan())
                  .hasPet(dto.getHasPet())
                  .petInfo(dto.getPetInfo())
                  .petCount(dto.getPetCount())
                  .indoorSmokingPlan(dto.getIndoorSmokingPlan())
                  .earlyTerminationRisk(dto.getEarlyTerminationRisk())
                  .nonresidentialUsePlan(
                          NonresidentialUsePlan.valueOf(dto.getNonresidentialUsePlan()))
                  .requestToOwner(dto.getRequestToOwner())
                  .checkedAt(dto.getCheckedAt())
                  .residentCount(dto.getResidentCount())
                  .occupation(dto.getOccupation())
                  .emergencyContact(dto.getEmergencyContact())
                  .relation(dto.getRelation())
                  .build();
      }
}
