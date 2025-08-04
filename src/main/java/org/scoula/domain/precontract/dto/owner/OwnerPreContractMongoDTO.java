package org.scoula.domain.precontract.dto.owner;

import java.util.List;

import org.scoula.domain.precontract.enums.ContractDuration;
import org.scoula.domain.precontract.enums.RentType;
import org.scoula.domain.precontract.enums.ResponsibilityParty;
import org.scoula.domain.precontract.enums.YesNoEnum;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "임대인 계약전 사전 정보 -> 몽고 DB 저장 DTO")
public class OwnerPreContractMongoDTO {
      private Long contractChatId;
      private Long userId;
      private RentType rentType;

      // flat 필드
      private Boolean mortgaged;
      private ContractDuration contractDuration;
      private YesNoEnum renewalIntent;
      private ResponsibilityParty responseRepairingFixtures;

      private List<String> restoreCategories;
      private Boolean hasConditionLog;
      private Boolean hasPenalty;
      private Boolean hasPriorityForExtension;
      private Boolean hasAutoPriceAdjustment;
      private Boolean allowJeonseRightRegistration;

      private Boolean requireRentGuaranteeInsurance;
      private ResponsibilityParty insuranceBurden;
      private YesNoEnum hasNotice;
      private String ownerBankName;
      private String ownerBankAccountNumber;
      private Integer paymentDueDate;
      private Double lateFeeInterestRate;

      // nested 필드
      private OwnerContractStep1DTO contractStep1;
      private OwnerContractStep2DTO contractStep2;
      private OwnerLivingStep1DTO livingStep1;

      private ContractDocumentDTO contractDocument;
}
