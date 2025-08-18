package org.scoula.domain.precontract.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContractDuration {
      YEAR_1("1년 계약", 1),
      YEAR_2("2년 계약", 2),
      YEAR_3("3년 계약", 3),
      YEAR_4("4년 계약", 4),
      YEAR_5("5년 계약", 5);
      private final String displayName;
      private final int years;
}
