package org.scoula.domain.precontract.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.scoula.domain.precontract.dto.TenantStep1DTO;
import org.scoula.domain.precontract.dto.TenantStep2DTO;
import org.scoula.domain.precontract.dto.TenantStep3DTO;
import org.scoula.domain.precontract.enums.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantPreContractCheckVO {

      private Long contractChatId; // PK & FK
      private Long identityId; // FK
      private Long riskckId; // FK

      private RiskType riskType;
      private RentType rentType;
      private LocalDate expectedMoveInDate;
      private ContractDuration contractDuration;
      private RenewsalIntent renewalIntent;
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

      // 1. step1
      public static TenantPreContractCheckVO toStep1VO(TenantStep1DTO dto) {
          return TenantPreContractCheckVO.builder()
                  .expectedMoveInDate(dto.getExpectedMoveInDate())
                  .contractDuration(ContractDuration.valueOf(dto.getContractDuration()))
                  .renewalIntent(RenewsalIntent.valueOf(dto.getRenewalIntent()))
                  .build();
      }

      // 2. step2
      public static TenantPreContractCheckVO toStep2VO(TenantStep2DTO dto) {
          return TenantPreContractCheckVO.builder()
                  .facilityRepairNeeded(dto.getFacilityRepairNeeded())
                  .interiorCleaningNeeded(dto.getInteriorCleaningNeeded())
                  .applianceInstallationPlan(dto.getApplianceInstallationPlan())
                  .hasPet(dto.getHasPet())
                  .petInfo(dto.getPetInfo())
                  .petCount(dto.getPetCount())
                  .build();
      }

      // 3. step3
      public static TenantPreContractCheckVO toStep3VO(TenantStep3DTO dto) {
          return TenantPreContractCheckVO.builder()
                  .indoorSmokingPlan(dto.getIndoorSmokingPlan())
                  .earlyTerminationRisk(dto.getEarlyTerminationRisk())
                  .nonresidentialUsePlan(
                          NonresidentialUsePlan.valueOf(dto.getNonresidentialUsePlan()))
                  .requestToOwner(dto.getRequestToOwner())
                  .residentCount(dto.getResidentCount())
                  .occupation(dto.getOccupation())
                  .emergencyContact(dto.getEmergencyContact())
                  .relation(dto.getRelation())
                  .build();
      }
}
