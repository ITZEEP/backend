package org.scoula.domain.chat.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.domain.chat.dto.ChatRoomInfoDto;
import org.scoula.domain.chat.vo.ChatRoom;

@Mapper
public interface ChatRoomMapper {
      ChatRoom findById(Long chatRoomId);

      Long findPropertyOwnerId(Long propertyId);

      List<ChatRoom> findByOwnerId(Long ownerId);

      List<ChatRoom> findByBuyerId(Long buyerId);

      ChatRoom findByUserAndHome(
              @Param("ownerId") Long ownerId,
              @Param("buyerId") Long buyerId,
              @Param("homeId") Long homeId);

      void insertChatRoom(ChatRoom chatRoom);

      void updateLastMessage(
              @Param("chatRoomId") Long chatRoomId,
              @Param("lastMessage") String lastMessage,
              @Param("lastMessageTime") LocalDateTime lastMessageTime);

      void incrementUnreadMessageCount(@Param("chatRoomId") Long chatRoomId);

      void resetUnreadMessageCount(@Param("chatRoomId") Long chatRoomId);

      void updateUnreadCount(@Param("chatRoomId") Long chatRoomId, @Param("count") int count);

      ChatRoomInfoDto getChatRoomInfoWithProperty(
              @Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);
}
