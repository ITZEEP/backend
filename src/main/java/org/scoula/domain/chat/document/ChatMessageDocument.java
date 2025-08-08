package org.scoula.domain.chat.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDocument {
      @Id private String id;

      @Field("chatRoomId")
      private Long chatRoomId;

      private Long senderId;
      private Long receiverId;
      private Boolean isRead;
      private String type;
      private String content;
      private String fileUrl;
      private String sendTime;
}
