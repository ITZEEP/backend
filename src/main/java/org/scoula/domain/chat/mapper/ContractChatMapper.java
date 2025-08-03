package org.scoula.domain.chat.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.domain.chat.vo.ContractChat;

@Mapper
public interface ContractChatMapper {
      void createContractChat(ContractChat contractChat);

      ContractChat findByContractChatId(Long contractChatId);

      ContractChat findByUserAndHome(
              @Param("ownerId") Long ownerId,
              @Param("buyerId") Long buyerId,
              @Param("homeId") Long homeId);

      void updateLastMessage(
              @Param("contractChatId") Long contractChatId, @Param("lastMessage") String lastMessage);

      void updateStartTime(
              @Param("contractChatId") Long contractChatId, @Param("startTime") String startTime);

      void updateEndTime(
              @Param("contractChatId") Long contractChatId, @Param("endTime") String endTime);

      void clearTimePoints(@Param("contractChatId") Long contractChatId);
}
