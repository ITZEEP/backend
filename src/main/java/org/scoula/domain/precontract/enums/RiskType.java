package org.scoula.domain.precontract.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RiskType {
      SAFE("안전"),
      WARN("주의"),
      DANGER("위험");

      private final String displayName;
}
