package org.scoula.domain.chat.dto;

import java.util.List;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpecialContractUserViewDto {
      private Long contractChatId;
      private Long round;
      private Integer totalClauses;
      private String userRole;
      private List<ClauseUserView> clauses;

      @Getter
      @Setter
      @NoArgsConstructor
      @AllArgsConstructor
      @Builder
      public static class ClauseUserView {
          private Integer id;
          private String title;
          private String content;
          private String level;
          private String reason;
      }
}
