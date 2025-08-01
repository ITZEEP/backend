package org.scoula.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractChatMessageRequestDto {
      private Long contractChatId;
      private Long senderId;
      private Long receiverId;
      private String content;
}
