package org.scoula.domain.chat.document;

import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "SPECIAL_CONTRACT_SELECTION")
public class SpecialContractSelectionDocument {
      @Id String id;
      private Long contractChatId;
      private Map<Integer, Boolean> ownerSelections;
      private Map<Integer, Boolean> tenantSelections;
      private boolean ownerCompleted;
      private boolean tenantCompleted;
      private boolean processed;
}
