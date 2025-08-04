package org.scoula.domain.home.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum HomeStatus {
      AVAILABLE("입주가능"),
      RESERVED("예약중"),
      CONTRACTED("계약완료");

      private final String description;

      HomeStatus(String description) {
          this.description = description;
      }

      @JsonValue
      public String getDescription() {
          return description;
      }

      @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
      public static HomeStatus from(String value) {
          for (HomeStatus status : values()) {
              if (status.description.equals(value)) {
                  return status;
              }
          }
          throw new IllegalArgumentException("Unknown home status: " + value);
      }
}
