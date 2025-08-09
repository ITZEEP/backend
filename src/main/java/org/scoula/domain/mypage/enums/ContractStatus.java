package org.scoula.domain.mypage.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 계약 상태 Enum contract_chat 테이블의 status와 final_contract 테이블 연동 */
@Getter
@RequiredArgsConstructor
public enum ContractStatus {
      // 계약 진행 단계
      STEP0("STEP0", "계약 초기 단계"),
      STEP1("STEP1", "계약 1단계"),
      STEP2("STEP2", "계약 2단계"),
      STEP4("STEP4", "계약 최종 단계"),

      // 협상 라운드
      ROUND0("ROUND0", "초기 협상"),
      ROUND1("ROUND1", "1차 협상"),
      ROUND2("ROUND2", "2차 협상"),
      ROUND3("ROUND3", "3차 협상"),

      // 완료 상태 (final_contract 테이블에 존재할 때)
      COMPLETED("COMPLETED", "계약 완료");

      private final String value;
      private final String description;

      @JsonValue
      public String getValue() {
          return value;
      }

      /**
       * 문자열 값으로 ContractStatus 찾기
       *
       * @param value 상태 값
       * @return ContractStatus enum
       */
      @JsonCreator
      public static ContractStatus fromValue(String value) {
          if (value == null) {
              return null;
          }

          for (ContractStatus status : ContractStatus.values()) {
              if (status.value.equals(value)) {
                  return status;
              }
          }

          throw new IllegalArgumentException("Unknown ContractStatus: " + value);
      }

      /** 계약이 진행 중인지 확인 */
      public boolean isInProgress() {
          return this != COMPLETED;
      }

      /** 협상 단계인지 확인 */
      public boolean isNegotiationRound() {
          return this == ROUND0 || this == ROUND1 || this == ROUND2 || this == ROUND3;
      }

      /** 계약 단계인지 확인 */
      public boolean isContractStep() {
          return this == STEP0 || this == STEP1 || this == STEP2 || this == STEP4;
      }
}
