package org.scoula.domain.home.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResidenceType {
      OPEN_ONE_ROOM("오픈형 원룸"),
      SEPARATED_ONE_ROOM("분리형 원룸"),
      TWO_ROOM("투룸"),
      OFFICETEL("오피스텔"),
      APARTMENT("아파트"),
      HOUSE("주택");

      private final String displayName;

      public static ResidenceType fromDisplayName(String displayName) {
          for (ResidenceType type : ResidenceType.values()) {
              if (type.getDisplayName().equals(displayName)) {
                  return type;
              }
          }
          throw new IllegalArgumentException("Unknown residence type: " + displayName);
      }
}
