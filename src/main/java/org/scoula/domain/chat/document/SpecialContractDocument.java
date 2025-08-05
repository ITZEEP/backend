package org.scoula.domain.chat.document;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "SPECIAL_CONTRACT")
public class SpecialContractDocument {

      @Id private String id;

      private Long contractChatId;

      private Long round;

      private Integer totalClauses;

      private List<Clause> clauses;

      @Getter
      @Setter
      @NoArgsConstructor
      @AllArgsConstructor
      @Builder
      public static class Clause {
          private Integer order;
          private String title;
          private String content;
          private Assessment assessment;
      }

      @Getter
      @Setter
      @NoArgsConstructor
      @AllArgsConstructor
      @Builder
      public static class Assessment {
          private Evaluation owner;
          private Evaluation tenant;
      }

      @Getter
      @Setter
      @NoArgsConstructor
      @AllArgsConstructor
      @Builder
      public static class Evaluation {
          private String level;
          private String reason;
      }
}
