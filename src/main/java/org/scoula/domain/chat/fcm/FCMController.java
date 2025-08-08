package org.scoula.domain.chat.fcm;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequestMapping("/api/fcm")
@RequiredArgsConstructor
@Log4j2
public class FCMController {

      private final FCMService fcmService;

      @PostMapping("/token")
      public ResponseEntity<?> saveToken(@RequestBody Map<String, String> request) {
          try {
              String userIdStr = request.get("userId");
              String token = request.get("token");

              if (userIdStr == null || token == null) {
                  return ResponseEntity.badRequest().body(Map.of("error", "userId와 token이 필요합니다."));
              }

              Long userId = Long.parseLong(userIdStr);
              fcmService.saveUserToken(userId, token);

              return ResponseEntity.ok().body(Map.of("message", "토큰 등록 성공", "userId", userId));

          } catch (Exception e) {
              log.error("토큰 등록 실패", e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(Map.of("error", "토큰 등록 실패"));
          }
      }

      @GetMapping("/token/{userId}")
      public ResponseEntity<?> getToken(@PathVariable Long userId) {
          try {
              String token = fcmService.getUserToken(userId);

              if (token != null) {
                  return ResponseEntity.ok()
                          .body(
                                  Map.of(
                                          "userId",
                                          userId,
                                          "hasToken",
                                          true,
                                          "tokenPreview",
                                          token.substring(0, Math.min(20, token.length())) + "..."));
              } else {
                  return ResponseEntity.ok()
                          .body(
                                  Map.of(
                                          "userId",
                                          userId,
                                          "hasToken",
                                          false,
                                          "message",
                                          "등록된 토큰이 없습니다."));
              }

          } catch (Exception e) {
              log.error("토큰 조회 실패", e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(Map.of("error", "토큰 조회 실패"));
          }
      }
}
