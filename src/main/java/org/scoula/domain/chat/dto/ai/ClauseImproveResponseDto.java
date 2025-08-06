package org.scoula.domain.chat.dto.ai;

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
public class ClauseImproveResponseDto {
      private boolean success;
      private String message;
      private Data data;
      private Object error;
      private String timestamp;

      @Getter
      @Setter
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      public static class Data {
          private Long round;
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
