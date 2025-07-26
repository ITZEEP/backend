package org.scoula.domain.chat.vo;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class ChatRoom {
      private Long chatRoomId;
      private Long ownerId;
      private Long buyerId;
      private Long homeId;
      private LocalDateTime createdAt;
      private LocalDateTime lastMessageAt;
      private String lastMessage;
      private Integer unreadMessageCount;
}
