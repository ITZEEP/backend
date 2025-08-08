package org.scoula.domain.chat.dto;

import java.util.Map;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SpecialContractSelectionDto {
      private Long contractChatId;
      private Map<Integer, Boolean> ownerSelections;
      private Map<Integer, Boolean> tenantSelections;
}
