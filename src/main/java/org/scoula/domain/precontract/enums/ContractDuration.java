package org.scoula.domain.precontract.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContractDuration {
      YEAR_1("1년 계약"),
      YEAR_2("2년 계약"),
      YEAR_OVER_2("2년 이상 ");
      private final String displayName;
}
