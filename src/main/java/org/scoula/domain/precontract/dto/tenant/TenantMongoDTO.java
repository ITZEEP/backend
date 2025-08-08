package org.scoula.domain.precontract.dto.tenant;

import java.time.LocalDate;
import java.time.LocalDateTime;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(description = "임차인 계약전 사전 정보 -> 몽고 DB 저장 DTO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantMongoDTO {

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
      private Boolean hasParking;
      private Integer parkingCount;
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
}
