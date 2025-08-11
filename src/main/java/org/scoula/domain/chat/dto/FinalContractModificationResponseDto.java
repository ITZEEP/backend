package org.scoula.domain.chat.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinalContractModificationResponseDto {
      private Long contractChatId;
      private Integer clauseOrder;
      private boolean accepted;
}
