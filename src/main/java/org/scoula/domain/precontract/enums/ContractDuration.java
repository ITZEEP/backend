package org.scoula.domain.precontract.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContractDuration {
      YEAR_1("1년 계약"),
      YEAR_2("2년 계약"),
      YEAR_3("3년 계약 "),
      YEAR_4("4년 계약 "),
      YEAR_5("5년 계약 ");
      private final String displayName;
}
