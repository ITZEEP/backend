package org.scoula.domain.chat.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContractChatDocument {
      @Id private String id;

      @Field("contractChatId")
      private String contractChatId;

      private Long senderId;
      private Long receiverId;
      private String content;
      private String sendTime;
}
