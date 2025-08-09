package org.scoula.domain.chat.vo;

public enum NotificationType {
      CHAT("채팅 메시지"),
      CONTRACT_REQUEST("계약 요청"),
      CONTRACT_ACCEPT("계약 수락"),
      CONTRACT_REJECT("계약 거절"),
      SYSTEM("시스템 알림");

      private final String description;

      NotificationType(String description) {
          this.description = description;
      }

      public String getDescription() {
          return description;
      }

      public static String getDescriptionByType(String type) {
          try {
              return NotificationType.valueOf(type).getDescription();
          } catch (Exception e) {
              return type;
          }
      }
}
