package org.scoula.domain.chat.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomInfoDto {
      private Long chatRoomId;
      private Long ownerId;
      private Long buyerId;
      private Long propertyId;
      private String createdAt;
      private String lastMessageAt;
      private String lastMessage;
      private Integer unreadMessageCount;

      private String propertyTitle;
      private String propertyAddress;
      private String propertyImageUrl;
      private Integer propertyPrice;
      private String propertyType;
}
