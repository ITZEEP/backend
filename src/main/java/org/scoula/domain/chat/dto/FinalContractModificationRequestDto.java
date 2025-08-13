package org.scoula.domain.chat.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinalContractModificationRequestDto {
      private Integer clauseOrder;
      private String newTitle;
      private String newContent;
}
