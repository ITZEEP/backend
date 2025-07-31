package org.scoula.domain.precontract.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RenewalIntent {
      YES("갱신 의사 있음"),
      NO("갱신 의사 없음"),
      UNDECIDED("갱신 의사 미정");
      private final String displayName;
}
