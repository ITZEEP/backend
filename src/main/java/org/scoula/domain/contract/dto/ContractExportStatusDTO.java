package org.scoula.domain.contract.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 계약서 내보내기 진행 상태를 추적하는 DTO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContractExportStatusDTO {

      // 계약 정보
      private Long contractChatId;
      private Long ownerId;
      private Long buyerId;

      // 진행 상태
      private String currentStep; // preview, signature, password, complete

      // 서명 상태
      private boolean ownerSignatureCompleted;
      private boolean buyerSignatureCompleted;
      private List<String> ownerSignatures; // base64 encoded signatures
      private List<String> buyerSignatures;

      // 체크박스 상태
      private boolean ownerHasTaxArrears;
      private boolean ownerHasPriorFixedDate;
      private boolean ownerMediationAgree;
      private boolean buyerMediationAgree;

      // 암호 설정 상태
      private boolean ownerPasswordSet;
      private boolean buyerPasswordSet;

      // 최종 상태
      private boolean isCompleted;
      private String finalPdfUrl;

      // 타임스탬프
      private Long lastUpdated;

      /** 양측이 모두 서명을 완료했는지 확인 */
      public boolean isBothSignaturesCompleted() {
          return ownerSignatureCompleted && buyerSignatureCompleted;
      }

      /** 양측이 모두 암호를 설정했는지 확인 */
      public boolean isBothPasswordsSet() {
          return ownerPasswordSet && buyerPasswordSet;
      }

      /** 계약서 내보내기가 완료 가능한지 확인 */
      public boolean isReadyForCompletion() {
          return true;
      }
}
