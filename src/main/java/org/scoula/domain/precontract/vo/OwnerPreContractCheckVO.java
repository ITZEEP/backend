package org.scoula.domain.precontract.vo;

import java.util.List;

import org.scoula.domain.precontract.enums.ContractDuration;
import org.scoula.domain.precontract.enums.RentType;
import org.scoula.domain.precontract.enums.ResponsibilityParty;
import org.scoula.domain.precontract.enums.YesNoEnum;
import org.scoula.global.common.constant.Constants.DateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class OwnerPreContractCheckVO {
      private Long ownerPrecheckId;
      private Long contractChatId;
      private Long identityId;
      private RentType rentType;
      private Boolean mortgaged;
      private ContractDuration contractDuration;
      private YesNoEnum renewalIntent;
      private ResponsibilityParty responseRepairingFixtures;
      private List<RestoreCategoryVO> restoreCategories;
      private Boolean hasConditionLog;
      private Boolean hasPenalty;
      private Boolean hasPriorityForExtension;
      private Boolean hasAutoPriceAdjustment;

      private Boolean requireRentGuaranteeInsurance;
      private ResponsibilityParty insuranceBurden;
      private YesNoEnum hasNotice;
      private String ownerBankName;
      private String ownerBankAccountNumber;

      private String contractFileUrl;
      private DateTime checkedAt;
}
