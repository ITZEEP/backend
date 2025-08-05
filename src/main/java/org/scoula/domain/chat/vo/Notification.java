package org.scoula.domain.chat.vo;

import java.time.LocalDateTime;

import lombok.*;

@Getter
@Setter
@Builder
public class Notification {
      private Long notiId;
      private Long userId;
      private String title;
      private String content;
      private String type;
      private Long relatedId;
      private Boolean isRead;
      private LocalDateTime createAt;
      private String data;
}
