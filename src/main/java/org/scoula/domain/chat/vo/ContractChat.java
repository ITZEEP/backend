package org.scoula.domain.chat.vo;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class ContractChat {
      private Long contractChatId;
      private Long homeId;
      private Long ownerId;
      private Long buyerId;
      private LocalDateTime contractStartAt;
      private String lastMessage;
      private String startPoint;
      private String endPoint;
      private ContractStatus status; // 추가된 필드

      // ContractStatus enum 추가
      public enum ContractStatus {
          STEP0,
          STEP1,
          STEP2,
          ROUND0,
          ROUND1,
          ROUND2,
          ROUND3,
          STEP4
      }

      // 현재 라운드 번호 계산 메서드
      public Long getCurrentRound() {
          if (status == null) return 1L;

          switch (status) {
              case ROUND0:
                  return 1L;
              case ROUND1:
                  return 2L;
              case ROUND2:
                  return 3L;
              case ROUND3:
                  return 4L;
              default:
                  return 1L;
          }
      }

      // 라운드인지 확인하는 메서드
      public boolean isInRound() {
          return status != null
                  && (status == ContractStatus.ROUND0
                          || status == ContractStatus.ROUND1
                          || status == ContractStatus.ROUND2
                          || status == ContractStatus.ROUND3);
      }
}
