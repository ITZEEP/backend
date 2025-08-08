package org.scoula.domain.chat.controller;

import java.util.List;
import java.util.Map;

import org.scoula.domain.chat.dto.NotificationCreateRequestDto;
import org.scoula.domain.chat.dto.NotificationDto;
import org.scoula.domain.chat.dto.NotificationListResponseDto;
import org.scoula.global.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Api(tags = "알림 API", description = "사용자 알림 관리 및 조회")
@RequestMapping("/api/chat/notifications")
public interface NotificationController {

      @ApiOperation(
              value = "사용자 알림 목록 조회",
              notes = "로그인한 사용자의 알림 목록을 페이징으로 조회합니다. 타입별 필터링이 가능합니다.",
              response = ApiResponse.class)
      @GetMapping
      ResponseEntity<ApiResponse<NotificationListResponseDto>> getNotifications(
              @ApiParam(value = "페이지 번호 (0부터 시작)", defaultValue = "0", example = "0")
                      @RequestParam(defaultValue = "0")
                      int page,
              @ApiParam(value = "페이지당 항목 수", defaultValue = "20", example = "20")
                      @RequestParam(defaultValue = "20")
                      int size,
              @ApiParam(
                              value = "알림 타입 (ALL, CHAT, CONTRACT, SYSTEM)",
                              defaultValue = "ALL",
                              example = "ALL")
                      @RequestParam(defaultValue = "ALL")
                      String type,
              Authentication authentication);

      @ApiOperation(
              value = "최신 알림 조회",
              notes = "메인 화면이나 대시보드에서 표시할 최신 알림을 제한된 개수만큼 조회합니다.",
              response = ApiResponse.class)
      @GetMapping("/recent")
      ResponseEntity<ApiResponse<List<NotificationDto>>> getRecentNotifications(
              @ApiParam(value = "조회할 알림 개수", defaultValue = "5", example = "5")
                      @RequestParam(defaultValue = "5")
                      int limit,
              Authentication authentication);

      @ApiOperation(
              value = "읽지 않은 알림 수 조회",
              notes = "사용자의 읽지 않은 알림 총 개수를 조회합니다. 알림 뱃지 표시에 사용됩니다.",
              response = ApiResponse.class)
      @GetMapping("/unread-count")
      ResponseEntity<ApiResponse<Integer>> getUnreadCount(Authentication authentication);

      @ApiOperation(
              value = "특정 알림 읽음 처리",
              notes = "지정된 알림 ID의 알림을 읽음 상태로 변경합니다.",
              response = ApiResponse.class)
      @PostMapping("/{notiId}/read")
      ResponseEntity<ApiResponse<String>> markAsRead(
              @ApiParam(value = "읽음 처리할 알림 ID", required = true, example = "1") @PathVariable
                      Long notiId,
              Authentication authentication);

      @ApiOperation(
              value = "여러 알림 읽음 처리",
              notes = "선택된 여러 알림을 일괄로 읽음 상태로 변경합니다.",
              response = ApiResponse.class)
      @PostMapping("/read")
      ResponseEntity<ApiResponse<String>> markMultipleAsRead(
              @ApiParam(
                              value = "읽음 처리할 알림 ID 목록",
                              required = true,
                              example = "{\"notiIds\": [1, 2, 3]}")
                      @RequestBody
                      Map<String, List<Long>> request,
              Authentication authentication);

      @ApiOperation(
              value = "모든 알림 읽음 처리",
              notes = "사용자의 모든 읽지 않은 알림을 읽음 상태로 변경합니다.",
              response = ApiResponse.class)
      @PostMapping("/read-all")
      ResponseEntity<ApiResponse<String>> markAllAsRead(Authentication authentication);

      @ApiOperation(
              value = "특정 알림 삭제",
              notes = "지정된 알림 ID의 알림을 완전히 삭제합니다. 삭제된 알림은 복구할 수 없습니다.",
              response = ApiResponse.class)
      @DeleteMapping("/{notiId}")
      ResponseEntity<ApiResponse<String>> deleteNotification(
              @ApiParam(value = "삭제할 알림 ID", required = true, example = "1") @PathVariable
                      Long notiId,
              Authentication authentication);

      @ApiOperation(
              value = "여러 알림 삭제",
              notes = "선택된 여러 알림을 일괄로 삭제합니다. 삭제된 알림은 복구할 수 없습니다.",
              response = ApiResponse.class)
      @DeleteMapping
      ResponseEntity<ApiResponse<String>> deleteMultipleNotifications(
              @ApiParam(value = "삭제할 알림 ID 목록", required = true, example = "{\"notiIds\": [1, 2, 3]}")
                      @RequestBody
                      Map<String, List<Long>> request,
              Authentication authentication);

      @ApiOperation(
              value = "모든 알림 삭제",
              notes = "사용자의 모든 알림을 완전히 삭제합니다. 삭제된 알림은 복구할 수 없습니다.",
              response = ApiResponse.class)
      @DeleteMapping("/all")
      ResponseEntity<ApiResponse<String>> deleteAllNotifications(Authentication authentication);

      @ApiOperation(
              value = "백그라운드 알림 저장",
              notes = "Service Worker에서 백그라운드 상태에서 받은 알림을 데이터베이스에 저장합니다.",
              response = ApiResponse.class)
      @PostMapping("/save-background")
      ResponseEntity<ApiResponse<String>> saveBackgroundNotification(
              @ApiParam(value = "백그라운드 알림 생성 요청 데이터", required = true) @RequestBody
                      NotificationCreateRequestDto requestDto);

      @ApiOperation(
              value = "알림 통계 조회",
              notes = "사용자의 알림 관련 전체 통계 정보를 조회합니다. (총 알림 수, 읽지 않은 수, 타입별 분포 등)",
              response = ApiResponse.class)
      @GetMapping("/stats")
      ResponseEntity<ApiResponse<Map<String, Object>>> getNotificationStats(
              Authentication authentication);

      @ApiOperation(
              value = "알림 타입별 통계 조회",
              notes = "사용자의 알림을 타입별로 분류하여 각 타입의 통계 정보를 조회합니다.",
              response = ApiResponse.class)
      @GetMapping("/stats/types")
      ResponseEntity<ApiResponse<Map<String, Object>>> getNotificationStatsByType(
              Authentication authentication);
}
