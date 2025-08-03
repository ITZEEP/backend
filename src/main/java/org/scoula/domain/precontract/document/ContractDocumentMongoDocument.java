package org.scoula.domain.precontract.document;

import java.util.List;

import org.scoula.domain.precontract.dto.owner.ContractDocumentDTO;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.*;

@Document(collection = "CONTRACT_DOCUMENT")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractDocumentMongoDocument {

      @Id private String id;

      private Long contractChatId;
      private Long ownerPrecheckId;
      private List<String> specialTerms;

      public static ContractDocumentMongoDocument from(
              Long contractChatId, Long userId, ContractDocumentDTO dto) {
          return ContractDocumentMongoDocument.builder()
                  .id(contractChatId + "_" + userId)
                  .contractChatId(contractChatId)
                  .ownerPrecheckId(dto.getOwnerPrecheckId())
                  .specialTerms(dto.getSpecialTerms())
                  .build();
      }
}
