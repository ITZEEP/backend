package org.scoula.domain.contract.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(description = "AI에서 가져온 적법성 검사")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalityDTO {
      // ⬇⬇ 샘플 JSON의 최상위 구조와 동일
      private Boolean success;
      private String message;
      private Payload data; // 기존 Data → Payload로 명칭만 변경 (상관없음)
      private Object error; // null 또는 객체/문자열일 수 있어 Object 권장
      private String timestamp; // "2025-08-11T14:39:41" 같은 문자열

      @lombok.Data
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      public static class Payload {
          private Boolean success;

          @JsonProperty("contract_chat_id")
          private Long contractChatId;

          @JsonProperty("validation_status")
          private String validationStatus;

          @JsonProperty("total_violations")
          private Integer totalViolations;

          @JsonProperty("violation_summary")
          private ViolationSummary violationSummary; // 샘플엔 없지만 올 수 있으니 optional

          private List<Violation> violations;

          @JsonProperty("validated_at")
          private String validatedAt;

          private String recommendation; // optional
      }

      @lombok.Data
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      public static class ViolationSummary {
          @JsonProperty("illegal_count")
          private Integer illegalCount;

          @JsonProperty("caution_count")
          private Integer cautionCount;
      }

      @lombok.Data
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      public static class Violation {
          @JsonProperty("violation_type")
          private String violationType;

          @JsonProperty("law_name")
          private String lawName;

          @JsonProperty("violation_content")
          private String violationContent;

          private String explanation;

          @JsonProperty("legal_basis")
          private String legalBasis;

          @JsonProperty("improvement_example")
          private String improvementExample;

          @JsonProperty("original_clause")
          private String originalClause;
      }
}
