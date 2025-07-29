package org.scoula.domain.precontract.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NonresidentialUsePlan {
      BUSINESS("사업 목적"),
      LODGING("숙박 목적"),
      NONE("다른 목적으로 사용 계획 없음");
      private final String displayName;
}
