package org.scoula.domain.precontract.dto.ai;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClauseRecommendRequestDto {
      private OcrData ocrData;
      private OwnerData ownerData;
      private TenantData tenantData;

      @Getter
      @Setter
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      public static class OcrData {
          private String extractedAt;
          private String fileName;
          private String rawText;
          private String source;
          private List<String> specialTerms;
      }

      @Getter
      @Setter
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      public static class OwnerData {
          private String checkedAt;
          private Long contractChatId;
          private String contractDuration;
          private Boolean hasAutoPriceAdjustment;
          private Boolean hasConditionLog;
          private String hasNotice;
          private Boolean hasPenalty;
          private Boolean hasPriorityForExtension;
          private Long identityId;
          private String insuranceBurden;
          private Boolean isMortgaged;
          private String ownerAccountNumber;
          private String ownerBankName;
          private Long ownerPrecheckId;
          private String renewalIntent;
          private String rentType;
          private Boolean requireRentGuaranteeInsurance;
          private String responseRepairingFixtures;
          private List<RestoreCategory> restoreCategories;
          private WolseInfo wolseInfo;
      }

      @Getter
      @Setter
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      public static class RestoreCategory {
          private Long restoreCategoryId;
          private String restoreCategoryName;
      }

      @Getter
      @Setter
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      public static class WolseInfo {
          private Double lateFeeInterestRate;
          private Integer paymentDueDay;
      }

      @Getter
      @Setter
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      public static class TenantData {
          private Long contractChatId;
          private String rentType;
          private Boolean loanPlan;
          private Boolean insurancePlan;
          private String expectedMoveInDate;
          private String contractDuration;
          private String renewalIntent;
          private Boolean facilityRepairNeeded;
          private Boolean interiorCleaningNeeded;
          private Boolean applianceInstallationPlan;
          private Boolean hasPet;
          private Boolean indoorSmokingPlan;
          private Boolean earlyTerminationRisk;
          private String checkedAt;
          private Integer residentCount;
          private String occupation;
          private String emergencyContact;
          private String relation;
      }
}
