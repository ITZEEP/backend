package org.scoula.global.websocket.controller;

import java.util.Map;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Api(tags = "웹소켓 채팅 API", description = "실시간 채팅 기능을 위한 웹소켓 엔드포인트")
public interface ChatController {

      @ApiOperation(value = "공개 채팅 메시지 전송", notes = "공개 채팅방에 메시지를 전송합니다")
      @MessageMapping("/chat.sendMessage")
      @SendTo("/topic/public")
      Object sendMessage(
              @ApiParam(value = "채팅 메시지 정보", required = true) @Payload
                      Map<String, String> chatMessage);

      @ApiOperation(value = "공개 채팅방 입장", notes = "사용자가 공개 채팅방에 입장합니다")
      @MessageMapping("/chat.addUser")
      @SendTo("/topic/public")
      Object addUser(
              @ApiParam(value = "사용자 정보", required = true) @Payload Map<String, String> chatMessage,
              SimpMessageHeaderAccessor headerAccessor);

      @ApiOperation(value = "특정 채팅방 메시지 전송", notes = "특정 채팅방에 메시지를 전송합니다")
      @MessageMapping("/chat/{roomId}/sendMessage")
      @SendTo("/topic/room/{roomId}")
      Object sendRoomMessage(
              @ApiParam(value = "채팅방 ID", required = true) @DestinationVariable String roomId,
              @ApiParam(value = "채팅 메시지 정보", required = true) @Payload
                      Map<String, String> chatMessage);

      @ApiOperation(value = "개인 메시지 전송", notes = "특정 사용자에게 개인 메시지를 전송합니다")
      @MessageMapping("/chat.sendPrivate")
      void sendPrivateMessage(
              @ApiParam(value = "개인 메시지 정보", required = true) @Payload
                      Map<String, String> chatMessage);

      @ApiOperation(value = "타이핑 상태 전송", notes = "채팅방에서 타이핑 상태를 전송합니다")
      @MessageMapping("/chat/{roomId}/typing")
      @SendTo("/topic/room/{roomId}/typing")
      Object typing(
              @ApiParam(value = "채팅방 ID", required = true) @DestinationVariable String roomId,
              @ApiParam(value = "타이핑 정보", required = true) @Payload
                      Map<String, String> typingMessage);

      @ApiOperation(value = "특정 채팅방 입장", notes = "사용자가 특정 채팅방에 입장합니다")
      @MessageMapping("/chat/{roomId}/join")
      @SendTo("/topic/room/{roomId}")
      Object joinRoom(
              @ApiParam(value = "채팅방 ID", required = true) @DestinationVariable String roomId,
              @ApiParam(value = "사용자 정보", required = true) @Payload Map<String, String> userMessage,
              SimpMessageHeaderAccessor headerAccessor);
}
