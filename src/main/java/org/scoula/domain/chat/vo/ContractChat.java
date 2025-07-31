package org.scoula.domain.chat.vo;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class ContractChat {
      private Long contractChatId;
      private Long homeId;
      private Long ownerId;
      private Long buyerId;
      private LocalDateTime contractStartAt;
      private String lastMessage;
      private String startPoint;
      private String endPoint;
}
