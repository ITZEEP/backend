package org.scoula.domain.chat.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.scoula.domain.chat.dto.ChatMessageRequestDto;
import org.scoula.domain.chat.dto.ChatRoomInfoDto;
import org.scoula.domain.chat.dto.ChatRoomWithUserInfoDto;
import org.scoula.global.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(tags = "채팅 API", description = "실시간 채팅, 채팅방 관리")
public interface ChatController {

      @ApiOperation(value = "메시지 전송 (WebSocket)")
      @MessageMapping("/chat/send")
      void sendMessage(@Payload ChatMessageRequestDto message, Principal principal);

      @ApiOperation(value = "채팅방 입장 (WebSocket)", notes = "사용자가 특정 채팅방에 입장할 때 호출")
      @MessageMapping("/chat/enter")
      void enterChatRoom(@Payload Map<String, Long> payload);

      @ApiOperation(value = "채팅방 퇴장 (WebSocket)", notes = "사용자가 채팅방을 떠날 때 호출")
      @MessageMapping("/chat/leave")
      void leaveChatRoom(@Payload Map<String, Long> payload);

      @ApiOperation(value = "사용자 완전 오프라인 (WebSocket)", notes = "앱 종료 등으로 완전히 오프라인 상태가 될 때 호출")
      @MessageMapping("/user/offline")
      void setUserOffline(@Payload Map<String, Long> payload);

      @ApiOperation(value = "온라인 상태 업데이트 (WebSocket)", notes = "사용자의 온라인/오프라인 상태를 업데이트")
      @MessageMapping("/user/online")
      void updateOnlineStatus(@Payload Map<String, Object> payload);

      @ApiOperation(value = "채팅방 생성", notes = "매물 ID로 채팅방을 생성합니다.", response = ApiResponse.class)
      @PostMapping("/rooms")
      ResponseEntity<ApiResponse<Long>> createChatRoom(
              @RequestParam Long propertyId, Authentication authentication);

      @ApiOperation(value = "임대인 채팅방 목록")
      @GetMapping("/rooms/owner")
      ResponseEntity<ApiResponse<List<ChatRoomWithUserInfoDto>>> getOwnerRooms(
              Authentication authentication);

      @ApiOperation(value = "임차인 채팅방 목록")
      @GetMapping("/rooms/buyer")
      ResponseEntity<ApiResponse<List<ChatRoomWithUserInfoDto>>> getBuyerRooms(
              Authentication authentication);

      @ApiOperation(value = "채팅 메시지 목록 조회")
      @GetMapping("/messages/{chatRoomId}")
      ResponseEntity<ApiResponse<?>> getMessages(
              @PathVariable Long chatRoomId, Authentication authentication);

      @ApiOperation(value = "채팅 파일 업로드", notes = "채팅방에서 사용할 파일을 업로드합니다.")
      @PostMapping("/upload")
      ResponseEntity<ApiResponse<?>> uploadChatFile(
              @RequestParam("file") MultipartFile file,
              @RequestParam("chatRoomId") Long chatRoomId,
              @RequestParam("receiverId") Long receiverId,
              Authentication authentication);

      @ApiOperation(value = "채팅방 미디어 파일 목록 조회", notes = "채팅방의 이미지와 동영상 파일만 조회합니다.")
      @GetMapping("/media/{chatRoomId}")
      ResponseEntity<ApiResponse<List<Map<String, Object>>>> getChatMediaFiles(
              @PathVariable Long chatRoomId,
              @RequestParam(defaultValue = "0") int page,
              @RequestParam(defaultValue = "20") int size,
              @RequestParam(defaultValue = "ALL") String mediaType,
              Authentication authentication);

      @ApiOperation(value = "채팅방 읽음 처리", notes = "특정 채팅방의 모든 메시지를 읽음으로 처리합니다.")
      @PostMapping("/rooms/{chatRoomId}/read")
      ResponseEntity<ApiResponse<String>> markChatRoomAsRead(
              @PathVariable Long chatRoomId, Authentication authentication);

      @ApiOperation(value = "현재 로그인된 사용자 ID 조회", notes = "Access Token을 기반으로 userId를 반환합니다.")
      @GetMapping("/user")
      ResponseEntity<ApiResponse<Map<String, Long>>> getCurrentUserId(Authentication authentication);

      @ApiOperation(value = "채팅 시스템 상태 조회 (디버깅용)", notes = "현재 온라인 사용자와 채팅방 접속 상태를 조회합니다.")
      @GetMapping("/debug/status")
      ResponseEntity<ApiResponse<Map<String, Object>>> getDebugStatus(Authentication authentication);

      @ApiOperation(value = "채팅방 상세 정보 조회", notes = "채팅방 ID로 채팅방의 상세 정보와 연결된 매물 정보를 조회합니다.")
      @GetMapping("/rooms/{chatRoomId}/info")
      ResponseEntity<ApiResponse<ChatRoomInfoDto>> getChatRoomInfo(
              @PathVariable Long chatRoomId, Authentication authentication);

      @ApiOperation(value = "계약 요청하기", notes = "구매자가 계약을 요청합니다.")
      @PostMapping("/rooms/{chatRoomId}/contract-request")
      ResponseEntity<ApiResponse<String>> sendContractRequest(
              @PathVariable Long chatRoomId, Authentication authentication);

      @ApiOperation(value = "계약 수락하기", notes = "매물 소유자가 계약 요청을 수락하고 계약 채팅방을 생성합니다.")
      @PostMapping("/rooms/{chatRoomId}/contract-accept")
      ResponseEntity<ApiResponse<Long>> acceptContractRequest(
              @PathVariable Long chatRoomId, Authentication authentication);

      @ApiOperation(value = "계약 요청 거절하기", notes = "매물 소유자가 계약 요청을 거절합니다.")
      @PostMapping("/rooms/{chatRoomId}/contract-reject")
      ResponseEntity<ApiResponse<String>> rejectContractRequest(
              @PathVariable Long chatRoomId, Authentication authentication);
}
