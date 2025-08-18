package org.scoula.domain.contract.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SignedType {
      TAX("미납 국세, 지방세 사인"),
      PRIORITY("선순위 확정일자 현황"),
      OWNER_CONTRACT("암대인 최종 사인"),
      BUYER_CONTRACT("임차인 최종 사");

      private final String displayName;
}
