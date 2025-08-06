package org.scoula.domain.precontract.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResponsibilityParty {
      OWNER("임대인"),
      TENANT("임차인"),
      UNDECIDED("일부 부담");

      private final String displayName;
}
