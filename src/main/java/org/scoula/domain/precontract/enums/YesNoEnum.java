package org.scoula.domain.precontract.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum YesNoEnum {
      YES("예"),
      NO("아니오"),
      UNDECIDED("미정");

      private final String displayName;
}
