package org.scoula.domain.home.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum HomeDirection {
      E("동향"),
      W("서향"),
      S("남향"),
      N("북향"),
      SE("남동향"),
      SW("남서향"),
      NE("북동향"),
      NW("북서향");

      private final String description;

      HomeDirection(String description) {
          this.description = description;
      }

      @JsonValue
      public String getDescription() {
          return description;
      }

      @JsonCreator
      public static HomeDirection from(String value) {
          for (HomeDirection direction : HomeDirection.values()) {
              if (direction.description.equals(value)) {
                  return direction;
              }
          }
          throw new IllegalArgumentException("Unknown home direction: " + value);
      }
}
