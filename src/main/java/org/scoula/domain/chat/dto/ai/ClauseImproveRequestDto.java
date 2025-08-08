package org.scoula.domain.chat.dto.ai;

import java.util.List;

import org.scoula.domain.chat.dto.ContentDataDto;

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
public class ClauseImproveRequestDto {
      private Long contractChatId;
      private OcrData ocrData;
      private Long round;
      private Long order;
      private OwnerData ownerData;
      private TenantData tenantData;
      private List<ContentDataDto> prevData;
      private ContentDataDto recentData;

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
          private JeonseInfo jeonseInfo;
          private WolseInfo wolseInfo;
          private String ownerAccountNumber;
          private String ownerBankName;
          private Long ownerPrecheckId;
          private String renewalIntent;
          private String rentType;
          private Boolean requireRentGuaranteeInsurance;
          private String responseRepairingFixtures;
          private List<RestoreCategory> restoreCategories;
      }

      @Getter
      @Setter
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      public static class JeonseInfo {
          private Boolean allowJeonseRightRegistration;
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
      public static class RestoreCategory {
          private Long restoreCategoryId;
          private String restoreCategoryName;
      }

      @Getter
      @Setter
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      public static class PrevClause {
          private String title;
          private String content;
          private String messages;
      }

      @Getter
      @Setter
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      public static class RecentClause {
          private String title;
          private String content;
          private String messages;
      }

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
      public static class TenantData {
          private String applianceInstallationPlan;
          private String checkedAt;
          private Long contractChatId;
          private String contractDuration;
          private String earlyTerminationRisk;
          private String emergencyContact;
          private String expectedMoveInDate;
          private String facilityRepairNeeded;
          private String hasPet;
          private Long identityId;
          private String indoorSmokingPlan;
          private String insurancePlan;
          private String interiorCleaningNeeded;
          private String loanPlan;
          private String occupation;
          private Integer petCount;
          private String petInfo;
          private String relation;
          private String renewalIntent;
          private String rentType;
          private String requestToOwner;
          private Integer residentCount;
      }
}
