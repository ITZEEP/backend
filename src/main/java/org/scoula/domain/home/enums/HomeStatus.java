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
          if (value == null || value.trim().isEmpty()) {
              throw new IllegalArgumentException("매물 상태 값이 null이거나 비어있습니다.");
          }

          String trimmedValue = value.trim();
          for (HomeStatus status : values()) {
              if (status.description.equals(trimmedValue)) {
                  return status;
              }
          }
          throw new IllegalArgumentException("알 수 없는 매물 상태입니다: " + value);
      }
}
