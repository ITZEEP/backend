package org.scoula.domain.chat.document;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "FINAL_SPECIAL_CONTRACT")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinalSpecialContractDocument {
      @Id private String id;
      private Long contractChatId;
      private Integer totalFinalClauses;
      private List<FinalClause> finalClauses;

      @Data
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      public static class FinalClause {
          private Integer order;
          private String title;
          private String content;
      }
}
