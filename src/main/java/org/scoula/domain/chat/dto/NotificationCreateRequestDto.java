package org.scoula.domain.chat.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationCreateRequestDto {
      private Long userId;
      private String title;
      private String content;
      private String type;
      private Long relatedId;
      private String data;
}
