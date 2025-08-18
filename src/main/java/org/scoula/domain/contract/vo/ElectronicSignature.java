package org.scoula.domain.contract.vo;

import java.time.LocalDateTime;

import org.scoula.domain.contract.enums.SignedType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ElectronicSignature {

      private Long signatureId;
      private Long contractId;
      private Long identityVerificationId;

      private String signatureFileKey;
      private String signatureFileHash;
      private SignedType signedType;

      private LocalDateTime createdAt;
}
