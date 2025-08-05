package org.scoula.domain.chat.controller; // NotificationControllerImpl.java - 컨트롤러 구현체

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.scoula.domain.chat.dto.NotificationCreateRequestDto;
import org.scoula.domain.chat.dto.NotificationDto;
import org.scoula.domain.chat.dto.NotificationListResponseDto;
import org.scoula.domain.chat.exception.ChatErrorCode;
import org.scoula.domain.chat.service.NotificationServiceInterface;
import org.scoula.domain.user.service.UserServiceInterface;
import org.scoula.domain.user.vo.User;
import org.scoula.global.common.dto.ApiResponse;
import org.scoula.global.common.exception.BusinessException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequestMapping("/api/chat/notifications")
@RequiredArgsConstructor
@Log4j2
public class NotificationControllerImpl implements NotificationController {

      private final NotificationServiceInterface notificationService;
      private final UserServiceInterface userService;

      @Override
      @GetMapping
      public ResponseEntity<ApiResponse<NotificationListResponseDto>> getNotifications(
              @RequestParam(defaultValue = "0") int page,
              @RequestParam(defaultValue = "20") int size,
              @RequestParam(required = false) String type,
              Authentication authentication) {

          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
          }

          User currentUser = currentUserOpt.get();
          Long userId = currentUser.getUserId();
          try {

              NotificationListResponseDto response;
              if (type != null && !type.trim().isEmpty()) {
                  response =
                          notificationService.getNotificationsByType(
                                  userId, type.toUpperCase(), page, size);
              } else {
                  response = notificationService.getNotifications(userId, page, size);
              }

              return ResponseEntity.ok(ApiResponse.success(response));

          } catch (Exception e) {
              log.error("알림 목록 조회 실패: page={}, size={}, type={}", page, size, type, e);
              return ResponseEntity.ok(ApiResponse.error("알림 목록을 불러올 수 없습니다."));
          }
      }

      @Override
      @GetMapping("/recent")
      public ResponseEntity<ApiResponse<List<NotificationDto>>> getRecentNotifications(
              @RequestParam(defaultValue = "5") int limit, Authentication authentication) {

          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
          }

          User currentUser = currentUserOpt.get();
          Long userId = currentUser.getUserId();
          try {
              List<NotificationDto> notifications =
                      notificationService.getLatestNotifications(userId, limit);

              return ResponseEntity.ok(ApiResponse.success(notifications));

          } catch (Exception e) {
              log.error("최신 알림 조회 실패: limit={}", limit, e);
              return ResponseEntity.ok(ApiResponse.error("최신 알림을 불러올 수 없습니다."));
          }
      }

      @Override
      @GetMapping("/unread-count")
      public ResponseEntity<ApiResponse<Integer>> getUnreadCount(Authentication authentication) {
          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
          }

          User currentUser = currentUserOpt.get();
          Long userId = currentUser.getUserId();
          try {
              int unreadCount = notificationService.getUnreadCount(userId);

              return ResponseEntity.ok(ApiResponse.success(unreadCount));

          } catch (Exception e) {
              log.error("읽지 않은 알림 수 조회 실패", e);
              return ResponseEntity.ok(ApiResponse.error("읽지 않은 알림 수를 불러올 수 없습니다."));
          }
      }

      @Override
      @PostMapping("/{notiId}/read")
      public ResponseEntity<ApiResponse<String>> markAsRead(
              @PathVariable Long notiId, Authentication authentication) {
          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
          }

          User currentUser = currentUserOpt.get();
          Long userId = currentUser.getUserId();
          try {
              validateNotificationOwnership(notiId, userId);

              notificationService.markAsRead(notiId);

              return ResponseEntity.ok(ApiResponse.success("알림이 확인되어 삭제되었습니다."));

          } catch (BusinessException e) {
              log.warn("알림 읽음 처리 권한 없음: notiId={}, userId={}", notiId, userId);
              return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
          } catch (Exception e) {
              log.error("알림 읽음 처리 실패: notiId={}", notiId, e);
              return ResponseEntity.ok(ApiResponse.error("알림 읽음 처리에 실패했습니다."));
          }
      }

      @Override
      @PostMapping("/read")
      public ResponseEntity<ApiResponse<String>> markMultipleAsRead(
              @RequestBody Map<String, List<Long>> request, Authentication authentication) {
          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
          }

          User currentUser = currentUserOpt.get();
          Long userId = currentUser.getUserId();

          try {

              List<Long> notiIds = request.get("notiIds");
              if (notiIds == null || notiIds.isEmpty()) {
                  return ResponseEntity.ok(ApiResponse.error("읽음 처리할 알림을 선택해주세요."));
              }

              for (Long notiId : notiIds) {
                  validateNotificationOwnership(notiId, userId);
              }

              notificationService.markAsRead(notiIds);

              return ResponseEntity.ok(ApiResponse.success(notiIds.size() + "개의 알림이 확인되어 삭제되었습니다."));

          } catch (BusinessException e) {
              log.warn("알림 읽음 처리 권한 없음: userId={}", userId);
              return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
          } catch (Exception e) {
              log.error("여러 알림 읽음 처리 실패", e);
              return ResponseEntity.ok(ApiResponse.error("알림 읽음 처리에 실패했습니다."));
          }
      }

      @Override
      @PostMapping("/read-all")
      public ResponseEntity<ApiResponse<String>> markAllAsRead(Authentication authentication) {
          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
          }

          User currentUser = currentUserOpt.get();
          Long userId = currentUser.getUserId();
          try {
              notificationService.markAllAsRead(userId);

              return ResponseEntity.ok(ApiResponse.success("모든 알림이 확인되어 삭제되었습니다."));

          } catch (Exception e) {
              log.error("모든 알림 읽음 처리 실패", e);
              return ResponseEntity.ok(ApiResponse.error("알림 읽음 처리에 실패했습니다."));
          }
      }

      @Override
      @DeleteMapping("/{notiId}")
      public ResponseEntity<ApiResponse<String>> deleteNotification(
              @PathVariable Long notiId, Authentication authentication) {
          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
          }

          User currentUser = currentUserOpt.get();
          Long userId = currentUser.getUserId();
          try {
              validateNotificationOwnership(notiId, userId);

              notificationService.deleteNotification(notiId);

              return ResponseEntity.ok(ApiResponse.success("알림이 삭제되었습니다."));

          } catch (BusinessException e) {
              log.warn("알림 삭제 권한 없음: notiId={}, userId={}", notiId, userId);
              return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
          } catch (Exception e) {
              log.error("알림 삭제 실패: notiId={}", notiId, e);
              return ResponseEntity.ok(ApiResponse.error("알림 삭제에 실패했습니다."));
          }
      }

      @Override
      @DeleteMapping
      public ResponseEntity<ApiResponse<String>> deleteMultipleNotifications(
              @RequestBody Map<String, List<Long>> request, Authentication authentication) {

          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
          }

          User currentUser = currentUserOpt.get();
          Long userId = currentUser.getUserId();
          try {

              List<Long> notiIds = request.get("notiIds");
              if (notiIds == null || notiIds.isEmpty()) {
                  return ResponseEntity.ok(ApiResponse.error("삭제할 알림을 선택해주세요."));
              }

              for (Long notiId : notiIds) {
                  validateNotificationOwnership(notiId, userId);
              }

              notificationService.deleteNotifications(notiIds);

              return ResponseEntity.ok(ApiResponse.success(notiIds.size() + "개의 알림이 삭제되었습니다."));

          } catch (BusinessException e) {
              log.warn("알림 삭제 권한 없음: userId={}", userId);
              return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
          } catch (Exception e) {
              log.error("여러 알림 삭제 실패", e);
              return ResponseEntity.ok(ApiResponse.error("알림 삭제에 실패했습니다."));
          }
      }

      @Override
      @DeleteMapping("/all")
      public ResponseEntity<ApiResponse<String>> deleteAllNotifications(
              Authentication authentication) {
          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
          }

          User currentUser = currentUserOpt.get();
          Long userId = currentUser.getUserId();
          try {
              notificationService.deleteAllNotifications(userId);

              return ResponseEntity.ok(ApiResponse.success("모든 알림이 삭제되었습니다."));

          } catch (Exception e) {
              log.error("모든 알림 삭제 실패", e);
              return ResponseEntity.ok(ApiResponse.error("알림 삭제에 실패했습니다."));
          }
      }

      @Override
      @PostMapping("/save-background")
      public ResponseEntity<ApiResponse<String>> saveBackgroundNotification(
              @RequestBody NotificationCreateRequestDto requestDto) {
          log.warn("백그라운드 알림 저장 API 호출됨 - 프론트엔드에서 호출하고 있습니다!");

          StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
          for (int i = 0; i < Math.min(5, stackTrace.length); i++) {
              log.info(
                      "Stack[{}]: {}.{}() - line {}",
                      i,
                      stackTrace[i].getClassName(),
                      stackTrace[i].getMethodName(),
                      stackTrace[i].getLineNumber());
          }

          return ResponseEntity.ok(ApiResponse.success("백그라운드 알림 저장 비활성화됨 - 중복 방지"));
      }

      @Override
      @GetMapping("/stats")
      public ResponseEntity<ApiResponse<Map<String, Object>>> getNotificationStats(
              Authentication authentication) {
          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
          }

          User currentUser = currentUserOpt.get();
          Long userId = currentUser.getUserId();
          try {
              NotificationListResponseDto allNotifications =
                      notificationService.getNotifications(userId, 0, 1);
              int unreadCount = notificationService.getUnreadCount(userId);

              Map<String, Object> stats =
                      Map.of(
                              "totalCount",
                              allNotifications.getTotalCount(),
                              "unreadCount",
                              unreadCount,
                              "readCount",
                              0);
              return ResponseEntity.ok(ApiResponse.success(stats));

          } catch (Exception e) {
              log.error("알림 통계 조회 실패", e);
              return ResponseEntity.ok(ApiResponse.error("알림 통계를 불러올 수 없습니다."));
          }
      }

      @Override
      @GetMapping("/stats/types")
      public ResponseEntity<ApiResponse<Map<String, Object>>> getNotificationStatsByType(
              Authentication authentication) {
          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
          }

          User currentUser = currentUserOpt.get();
          Long userId = currentUser.getUserId();
          try {
              Map<String, Object> typeStats = calculateTypeStatistics(userId);

              return ResponseEntity.ok(ApiResponse.success(typeStats));

          } catch (Exception e) {
              log.error("타입별 알림 통계 조회 실패", e);
              return ResponseEntity.ok(ApiResponse.error("알림 통계를 불러올 수 없습니다."));
          }
      }

      private void validateNotificationOwnership(Long notiId, Long userId) {
          log.debug("알림 소유자 검증: notiId={}, userId={}", notiId, userId);
      }

      private Map<String, Object> calculateTypeStatistics(Long userId) {
          try {
              NotificationListResponseDto chatNotifications =
                      notificationService.getNotificationsByType(userId, "CHAT", 0, 1);
              NotificationListResponseDto contractRequestNotifications =
                      notificationService.getNotificationsByType(userId, "CONTRACT_REQUEST", 0, 1);
              NotificationListResponseDto contractAcceptNotifications =
                      notificationService.getNotificationsByType(userId, "CONTRACT_ACCEPT", 0, 1);
              NotificationListResponseDto contractRejectNotifications =
                      notificationService.getNotificationsByType(userId, "CONTRACT_REJECT", 0, 1);
              NotificationListResponseDto systemNotifications =
                      notificationService.getNotificationsByType(userId, "SYSTEM", 0, 1);

              int totalContractCount =
                      contractRequestNotifications.getTotalCount()
                              + contractAcceptNotifications.getTotalCount()
                              + contractRejectNotifications.getTotalCount();

              return Map.of(
                      "chatCount", chatNotifications.getTotalCount(),
                      "contractRequestCount", contractRequestNotifications.getTotalCount(),
                      "contractAcceptCount", contractAcceptNotifications.getTotalCount(),
                      "contractRejectCount", contractRejectNotifications.getTotalCount(),
                      "totalContractCount", totalContractCount,
                      "systemCount", systemNotifications.getTotalCount(),
                      "totalUnreadCount", notificationService.getUnreadCount(userId));

          } catch (Exception e) {
              log.error("타입별 통계 계산 실패: userId={}", userId, e);
              return Map.of(
                      "chatCount", 0,
                      "contractRequestCount", 0,
                      "contractAcceptCount", 0,
                      "contractRejectCount", 0,
                      "totalContractCount", 0,
                      "systemCount", 0,
                      "totalUnreadCount", 0);
          }
      }
}
