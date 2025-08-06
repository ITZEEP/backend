package org.scoula.domain.chat.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.scoula.domain.chat.document.ContractChatDocument;
import org.scoula.domain.chat.document.SpecialContractFixDocument;
import org.scoula.domain.chat.dto.ContractChatMessageRequestDto;
import org.scoula.domain.chat.dto.SpecialContractUserViewDto;
import org.scoula.domain.chat.dto.ai.ClauseImproveResponseDto;
import org.scoula.global.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(tags = "계약 채팅 API", description = "계약 채팅방 관리 및 특약 대화")
public interface ContractChatController {

      @ApiOperation(value = "계약 채팅방 생성", notes = "일반 채팅방에서 계약 시작하기 버튼을 눌렀을 때 계약 채팅방을 생성합니다.")
      @PostMapping("/rooms")
      ResponseEntity<ApiResponse<Long>> createContractChat(
              @RequestParam Long chatRoomId, Authentication authentication);

      @ApiOperation(value = "계약 채팅 메시지 전송 (WebSocket)")
      @MessageMapping("/contract/chat/send")
      void sendContractMessage(@Payload ContractChatMessageRequestDto message, Principal principal);

      @ApiOperation(value = "계약 채팅방 메시지 목록 조회")
      @GetMapping("/messages/{contractChatId}")
      ResponseEntity<ApiResponse<List<ContractChatDocument>>> getContractMessages(
              @PathVariable Long contractChatId, Authentication authentication);

      @ApiOperation(value = "특약 대화 시작점 설정", notes = "특약 대화 시작 버튼을 클릭했을 때 시작점을 설정합니다.")
      @PostMapping("/{contractChatId}/start-point")
      ResponseEntity<ApiResponse<String>> setStartPoint(
              @PathVariable Long contractChatId, Authentication authentication);

      @ApiOperation(value = "특약 대화 종료 요청(임대인)", notes = "특약 대화 종료를 요청합니다.")
      @PostMapping("/{contractChatId}/request-end")
      ResponseEntity<ApiResponse<String>> requestEndPointExport(
              @PathVariable Long contractChatId, Authentication authentication);

      @ApiOperation(value = "특약 대화 종료 요청 거절(임차인)", notes = "임차인이 특약 종료 요청을 거절합니다.")
      @PostMapping("/{contractChatId}/end-point-reject")
      ResponseEntity<ApiResponse<String>> rejectEndPointExport(
              @PathVariable Long contractChatId, Authentication authentication);

      @ApiOperation(value = "특약 대화 종료 및 내보내기", notes = "특약 대화 종료 버튼을 클릭했을 때 종료점을 설정하고 대화 내용을 내보냅니다.")
      @PostMapping("/{contractChatId}/end-point-export")
      ResponseEntity<ApiResponse<ClauseImproveResponseDto>> setEndPointAndExport(
              @PathVariable Long contractChatId,
              @RequestParam Long order,
              Authentication authentication);

      @ApiOperation(value = "계약 채팅방 입장 (WebSocket)", notes = "계약 채팅방에 입장할 때 온라인 상태를 설정합니다.")
      @MessageMapping("/contract/chat/enter")
      void enterContractChatRoom(@Payload Map<String, Long> payload, Principal principal);

      @ApiOperation(value = "계약 채팅방 퇴장 (WebSocket)", notes = "계약 채팅방에서 퇴장할 때 오프라인 상태를 설정합니다.")
      @MessageMapping("/contract/chat/leave")
      void leaveContractChatRoom(@Payload Map<String, Long> payload);

      @ApiOperation(value = "계약 채팅 사용자 오프라인 (WebSocket)", notes = "계약 채팅에서 완전 오프라인 상태로 전환합니다.")
      @MessageMapping("/contract/user/offline")
      void setContractUserOffline(@Payload Map<String, Long> payload);

      @ApiOperation(value = "계약 채팅방 온라인 상태 조회", notes = "계약 채팅방 참여자들의 온라인 상태를 조회합니다.")
      @GetMapping("/{contractChatId}/online-status")
      ResponseEntity<ApiResponse<Map<String, Object>>> getContractChatOnlineStatus(
              @PathVariable Long contractChatId, Authentication authentication);

      @ApiOperation(value = "계약 채팅방 정보 조회", notes = "계약 채팅방의 기본 정보를 조회합니다.")
      @GetMapping("/rooms/{contractChatId}/info")
      ResponseEntity<ApiResponse<Map<String, Object>>> getContractChatInfo(
              @PathVariable Long contractChatId, Authentication authentication);

      // 특약 관련 메서드

      @ApiOperation(
              value = "특약 선택 제출",
              notes = "현재 로그인한 사용자(임대인/임차인)의 Y/N 선택을 MongoDB에 저장합니다. 양쪽 모두 제출하면 특약 문서를 자동 생성합니다.")
      @PostMapping("/special-contracts/{contractChatId}/submit-selection")
      ResponseEntity<ApiResponse<Object>> submitSpecialContractSelection(
              @PathVariable Long contractChatId,
              @RequestBody Map<Integer, Boolean> selections,
              Authentication authentication);

      @ApiOperation(value = "특약 문서 생성", notes = "특약 협상을 위한 초기 문서를 생성합니다.")
      @PostMapping("/special-contract")
      ResponseEntity<ApiResponse<SpecialContractFixDocument>> createSpecialContract(
              @RequestParam Long contractChatId,
              @RequestParam Long order,
              Authentication authentication);

      @ApiOperation(value = "특약 문서 조회", notes = "특정 특약 문서의 상세 정보를 조회합니다.")
      @GetMapping("/special-contract/{contractChatId}")
      ResponseEntity<ApiResponse<SpecialContractUserViewDto>> getSpecialContractForUser(
              @PathVariable Long contractChatId, Authentication authentication);

      @ApiOperation(value = "특약 recentData 업데이트", notes = "현재 라운드의 특약 내용을 업데이트합니다.")
      @PutMapping("/special-contract/{contractChatId}/recent")
      ResponseEntity<ApiResponse<SpecialContractFixDocument>> updateRecentData(
              @PathVariable Long contractChatId,
              @RequestParam Long order,
              @RequestParam(required = false) String messages,
              Authentication authentication);

      @ApiOperation(value = "다음 라운드로 자동 진행", notes = "isPassed가 false인 모든 특약을 자동으로 다음 라운드로 진행합니다.")
      @PostMapping("/special-contract/{contractChatId}/next-round-auto")
      ResponseEntity<ApiResponse<List<SpecialContractFixDocument>>> proceedToNextRoundAuto(
              @PathVariable Long contractChatId, Authentication authentication);

      @ApiOperation(value = "특약 완료 처리", notes = "특약을 완료 상태로 변경합니다.")
      @PatchMapping("/special-contract/{contractChatId}/complete")
      ResponseEntity<ApiResponse<SpecialContractFixDocument>> completeSpecialContract(
              @PathVariable Long contractChatId,
              @RequestParam Long order,
              Authentication authentication);

      @ApiOperation(value = "특약 문서 존재 여부 확인", notes = "특약 문서의 존재 여부를 확인합니다.")
      @GetMapping("/special-contract/{contractChatId}/exists")
      ResponseEntity<ApiResponse<Boolean>> checkSpecialContractExists(
              @PathVariable Long contractChatId, Authentication authentication);

      @ApiOperation(value = "완료된 특약 문서 목록 조회", notes = "완료된 모든 특약 문서를 조회합니다.")
      @GetMapping("/special-contract/completed")
      ResponseEntity<ApiResponse<List<SpecialContractFixDocument>>> getCompletedSpecialContracts(
              Authentication authentication);

      @ApiOperation(value = "미완료 특약 문서 목록 조회", notes = "미완료된 모든 특약 문서를 조회합니다.")
      @GetMapping("/special-contract/{contractChatId}/incomplete")
      ResponseEntity<ApiResponse<List<SpecialContractFixDocument>>> getIncompleteSpecialContracts(
              @PathVariable Long contractChatId, Authentication authentication);
}
