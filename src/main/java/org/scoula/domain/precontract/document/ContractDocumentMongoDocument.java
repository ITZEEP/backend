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
      private Long userId;
      private Long ownerPrecheckId;
      private List<String> specialTerms;

      // AI OCR 분석 결과
      private String filename;
      private String documentType;
      private String extractedAt;
      private String source;
      private String rawText;

      public static ContractDocumentMongoDocument from(
              Long contractChatId, Long userId, ContractDocumentDTO dto) {
          return ContractDocumentMongoDocument.builder()
                  .id(contractChatId + "_" + userId)
                  .contractChatId(contractChatId)
                  .ownerPrecheckId(dto.getOwnerPrecheckId())
                  .userId(userId)
                  .specialTerms(dto.getSpecialTerms())
                  .filename(dto.getFilename())
                  .documentType(dto.getDocumentType())
                  .extractedAt(dto.getExtractedAt())
                  .source(dto.getSource())
                  .rawText(dto.getRawText())
                  .build();
      }
}
