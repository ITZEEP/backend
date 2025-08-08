package org.scoula.domain.chat.dto;

import java.util.List;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SpecialContractDto {
      private Long contractChatId;
      private Long order;
      private Long round;
      private List<ContentDataDto> prevData;
      private ContentDataDto recentData;
}
