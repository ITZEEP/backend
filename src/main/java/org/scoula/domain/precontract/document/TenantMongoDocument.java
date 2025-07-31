package org.scoula.domain.precontract.document;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.scoula.domain.precontract.dto.tenant.TenantMongoDTO;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "PRE_CONTRACT_BUYER")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantMongoDocument {

      @Id private String id;

      @Field("contractChatId")
      private Long contractChatId;

      private String rentType;

      // step 1
      private Boolean loanPlan;
      private Boolean insurancePlan;
      private LocalDate expectedMoveInDate;
      private String contractDuration;
      private String renewalIntent;

      // step 2
      private Boolean facilityRepairNeeded;
      private Boolean interiorCleaningNeeded;
      private Boolean applianceInstallationPlan;
      private Boolean hasPet;
      private String petInfo;
      private Long petCount;

      // step 3
      private Boolean indoorSmokingPlan;
      private Boolean earlyTerminationRisk;
      private String requestToOwner;
      private LocalDateTime checkedAt;
      private Integer residentCount;
      private String occupation;
      private String emergencyContact;
      private String relation;

      public static TenantMongoDocument toDocument(TenantMongoDTO dto) {
          return TenantMongoDocument.builder()
                  .contractChatId(dto.getContractChatId())
                  .rentType(dto.getRentType())
                  .loanPlan(dto.getLoanPlan())
                  .insurancePlan(dto.getInsurancePlan())
                  .expectedMoveInDate(dto.getExpectedMoveInDate())
                  .contractDuration(dto.getContractDuration())
                  .renewalIntent(dto.getRenewalIntent())
                  .facilityRepairNeeded(dto.getFacilityRepairNeeded())
                  .interiorCleaningNeeded(dto.getInteriorCleaningNeeded())
                  .applianceInstallationPlan(dto.getApplianceInstallationPlan())
                  .hasPet(dto.getHasPet())
                  .petInfo(dto.getPetInfo())
                  .petCount(dto.getPetCount())
                  .indoorSmokingPlan(dto.getIndoorSmokingPlan())
                  .earlyTerminationRisk(dto.getEarlyTerminationRisk())
                  .requestToOwner(dto.getRequestToOwner())
                  .checkedAt(dto.getCheckedAt())
                  .residentCount(dto.getResidentCount())
                  .occupation(dto.getOccupation())
                  .emergencyContact(dto.getEmergencyContact())
                  .relation(dto.getRelation())
                  .build();
      }
}
