package org.scoula.domain.contract.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalityDTO {
    private Boolean success;
    private String message;

    @JsonProperty("contract_chat_id")
    private Long contractChatId;

    @JsonProperty("validation_status")
    private String validationStatus;

    @JsonProperty("total_violations")
    private Integer totalViolations;

    private List<Violation> violations;  // 최상위에 바로 위치

    @JsonProperty("validated_at")
    private String validatedAt;

    private Object error;
    private String timestamp;

    @Data
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