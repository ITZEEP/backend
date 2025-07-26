package org.scoula.domain.chat.dto;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageRequestDto {
      private Long chatRoomId;
      private Long senderId;
      private Long receiverId;
      private String type;
      private String content;
      private String fileUrl;
}
