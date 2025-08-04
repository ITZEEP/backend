package org.scoula.domain.home.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ResidenceType {
      APARTMENT("아파트"),
      VILLA("빌라"),
      OFFICETEL("오피스텔"),
      HOUSE("단독주택"),
      OPEN_ONE_ROOM("오픈형 원룸"),
      SEPARATED_ONE_ROOM("분리형 원룸"),
      TWO_ROOM("투룸");

      private final String description;

      ResidenceType(String description) {
          this.description = description;
      }

      @JsonValue
      public String getDescription() {
          return description;
      }

      @JsonCreator
      public static ResidenceType from(String value) {
          for (ResidenceType type : ResidenceType.values()) {
              if (type.description.equals(value)) {
                  return type;
              }
          }
          throw new IllegalArgumentException("Unknown residence type: " + value);
      }
}
