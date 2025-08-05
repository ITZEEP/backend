package org.scoula.domain.precontract.dto.ai;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClauseRecommendResponseDto {
      private boolean success;
      private String message;
      private ClauseData data;
      private String timestamp;

      @Getter
      @Setter
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      public static class ClauseData {
          @JsonProperty("total_clauses")
          private Integer totalClauses;

          private List<Clause> clauses;
      }

      @Getter
      @Setter
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      public static class Clause {
          private Integer order;
          private String title;
          private String content;
          private Assessment assessment;
      }

      @Getter
      @Setter
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      public static class Assessment {
          private PartyAssessment owner;
          private PartyAssessment tenant;
      }

      @Getter
      @Setter
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      public static class PartyAssessment {
          private String level;
          private String reason;
      }
}
