package org.scoula.domain.precontract.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnerWolseInfoVO {
      private Long ownerWolseRentId;
      private Long ownerPrecheckId;
      private boolean adjustable;
      private boolean monthlyAdjustable;
      private Integer depositAdjustmentMin;
      private Integer rentAdjustmentMin;
      private Integer paymentDueDate;
      private Double lateFeeInterestRate;
}
