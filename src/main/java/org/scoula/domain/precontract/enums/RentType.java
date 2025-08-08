package org.scoula.domain.precontract.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RentType {
      JEONSE("전세"),
      WOLSE("월세");
      private final String displayName;
}
