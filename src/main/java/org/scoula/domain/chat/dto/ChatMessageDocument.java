package org.scoula.domain.chat.dto;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "MESSAGE")
@Data
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
