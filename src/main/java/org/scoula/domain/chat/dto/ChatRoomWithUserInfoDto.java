package org.scoula.domain.chat.dto;

import java.time.LocalDateTime;

import org.scoula.domain.chat.vo.ChatRoom;
import org.scoula.domain.user.vo.User;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomWithUserInfoDto {
      private Long chatRoomId;
      private Long ownerId;
      private Long buyerId;
      private Long homeId;
      private LocalDateTime createdAt;
      private LocalDateTime lastMessageAt;
      private String lastMessage;
      private Integer unreadMessageCount;

      private Long otherUserId;
      private String otherUserNickname;
      private String otherUserProfileUrl;

      public static ChatRoomWithUserInfoDto of(
              ChatRoom chatRoom, User otherUser, Long currentUserId) {
          Long otherUserId =
                  currentUserId.equals(chatRoom.getOwnerId())
                          ? chatRoom.getBuyerId()
                          : chatRoom.getOwnerId();

          return ChatRoomWithUserInfoDto.builder()
                  .chatRoomId(chatRoom.getChatRoomId())
                  .ownerId(chatRoom.getOwnerId())
                  .buyerId(chatRoom.getBuyerId())
                  .homeId(chatRoom.getHomeId())
                  .createdAt(chatRoom.getCreatedAt())
                  .lastMessageAt(chatRoom.getLastMessageAt())
                  .lastMessage(chatRoom.getLastMessage())
                  .unreadMessageCount(chatRoom.getUnreadMessageCount())
                  .otherUserId(otherUserId)
                  .otherUserNickname(otherUser.getNickname())
                  .otherUserProfileUrl(otherUser.getProfileImgUrl())
                  .build();
      }
}
