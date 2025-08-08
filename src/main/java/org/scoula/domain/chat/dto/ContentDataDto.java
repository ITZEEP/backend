package org.scoula.domain.chat.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentDataDto {
      private String title;
      private String content;
      private String messages;
}
