package org.scoula.domain.home.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum LeaseType {
      JEONSE("전세"),
      WOLSE("월세");

      private final String description;

      LeaseType(String description) {
          this.description = description;
      }

      @JsonValue
      public String getDescription() {
          return description;
      }

      @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
      public static LeaseType from(String value) {
          for (LeaseType type : values()) {
              if (type.description.equals(value)) {
                  return type;
              }
          }
          throw new IllegalArgumentException("Unknown lease type: " + value);
      }
}
