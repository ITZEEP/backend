package org.scoula.domain.chat.fcm;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.scoula.global.redis.service.RedisServiceInterface;
import org.springframework.stereotype.Service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class FCMService {

      private final RedisServiceInterface redisService;
      private static final String FCM_TOKEN_PREFIX = "fcm_token:";

      public void saveUserToken(Long userId, String token) {
          String key = FCM_TOKEN_PREFIX + userId;
          redisService.setStringValue(key, token, 60, TimeUnit.DAYS);
          log.info("FCM 토큰 저장 완료 - UserId: {}", userId);
      }

      public boolean sendNotification(
              Long userId, String title, String body, Map<String, String> data) {
          try {
              String token = getUserToken(userId);
              if (token == null) {
                  log.info("FCM 토큰을 찾을 수 없음. UserId: {}", userId);
                  return false;
              }

              Message message =
                      Message.builder()
                              .setNotification(
                                      Notification.builder().setTitle(title).setBody(body).build())
                              .putAllData(data != null ? data : new HashMap<>())
                              .setToken(token)
                              .build();

              String response = FirebaseMessaging.getInstance().send(message);
              log.info("알림 전송 성공 - UserId: {}, Response: {}", userId, response);
              return true;

          } catch (Exception e) {
              if (e instanceof FirebaseMessagingException) {
                  FirebaseMessagingException fme = (FirebaseMessagingException) e;

                  if (fme.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                      log.warn("유효하지 않은 FCM 토큰 감지 - UserId: {}, 토큰 삭제 처리", userId);
                      removeUserToken(userId);
                      return false;
                  }
              }

              log.error("알림 전송 실패 - UserId: {}", userId, e);
              return false;
          }
      }

      public void removeUserToken(Long userId) {
          String key = FCM_TOKEN_PREFIX + userId;
          boolean deleted = redisService.deleteValue(key);
          log.info("유효하지 않은 FCM 토큰 삭제 완료 - UserId: {}, 삭제됨: {}", userId, deleted);
      }

      public String getUserToken(Long userId) {
          String key = FCM_TOKEN_PREFIX + userId;
          return redisService.getStringValue(key);
      }
}
