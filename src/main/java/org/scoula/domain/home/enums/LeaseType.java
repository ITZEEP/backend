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
          if (value == null || value.trim().isEmpty()) {
              throw new IllegalArgumentException("임대 유형 값이 null이거나 비어있습니다.");
          }
          String trimmed = value.trim();

          // 이름 기반 매칭 (JEONSE, WOLSE)
          for (LeaseType type : values()) {
              if (type.name().equalsIgnoreCase(trimmed) || type.description.equals(trimmed)) {
                  return type;
              }
          }
          throw new IllegalArgumentException("알 수 없는 임대 유형입니다: " + value);
      }
}
