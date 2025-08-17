package org.scoula.domain.chat.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModificationRequestData {
      private String newTitle;
      private String newContent;
      private Long requesterId;
      private String createdAt;
}
