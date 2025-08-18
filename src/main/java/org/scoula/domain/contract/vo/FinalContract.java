package org.scoula.domain.contract.vo;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinalContract {

      private Long contractId;
      private Long homeId;
      private Long ownerId;
      private Long buyerId;

      private String contractPdfKey;
      private String contractPdfHash;
      private LocalDateTime contractDate;
      private LocalDateTime contractExpireDate;
      private LocalDateTime ownerIdentityVerifiedAt;
      private LocalDateTime buyerIdentityVerifiedAt;

      private int depositPrice;
      private int monthlyRent;
      private int maintenanceFee;
      private LocalDateTime createdAt;
}
