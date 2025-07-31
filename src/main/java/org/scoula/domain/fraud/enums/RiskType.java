package org.scoula.domain.fraud.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import io.swagger.annotations.ApiModel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@ApiModel(description = "사기 위험도 타입")
public enum RiskType {
      DANGER("DANGER", "위험", "높은 위험도가 감지되었습니다"),
      WARN("WARN", "경고", "주의가 필요한 사항이 있습니다"),
      SAFE("SAFE", "안전", "안전한 매물로 판단됩니다");

      private final String value;
      private final String description;
      private final String message;

      @JsonValue
      public String getValue() {
          return value;
      }

      @Override
      public String toString() {
          return this.value;
      }
}
