package org.scoula.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomUpdateDto {
      private Long roomId;
      private String lastMessage;
      private String timestamp;
      private int unreadCount;
      private Long senderId;
}
