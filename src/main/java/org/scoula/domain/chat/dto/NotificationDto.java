package org.scoula.domain.chat.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class NotificationDto {
      private Long notiId;
      private Long userId;
      private String title;
      private String content;
      private String type;
      private Long relatedId;
      private Boolean isRead;
      private LocalDateTime createAt;
      private String data;
      private String timeAgo;
      private String relatedInfo;
      private String typeDescription;
}
