package org.scoula.domain.precontract.document;

import org.scoula.domain.precontract.dto.owner.ContractDocumentDTO;
import org.scoula.domain.precontract.dto.owner.OwnerContractStep1DTO;
import org.scoula.domain.precontract.dto.owner.OwnerContractStep2DTO;
import org.scoula.domain.precontract.dto.owner.OwnerLivingStep1DTO;
import org.scoula.domain.precontract.dto.owner.OwnerPreContractMongoDTO;
import org.scoula.domain.precontract.enums.RentType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "PRE_CONTRACT_OWNER")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerMongoDocument {
      @Id private String id;

      private Long contractChatId;
      private Long userId;
      private RentType rentType;

      private OwnerContractStep1DTO contractStep1;
      private OwnerContractStep2DTO contractStep2;
      private OwnerLivingStep1DTO livingStep1;

      private ContractDocumentDTO contractDocument;

      public static OwnerMongoDocument from(OwnerPreContractMongoDTO dto) {
          return OwnerMongoDocument.builder()
                  .contractChatId(dto.getContractChatId())
                  .userId(dto.getUserId())
                  .rentType(dto.getRentType())
                  .contractStep1(dto.getContractStep1())
                  .contractStep2(dto.getContractStep2())
                  .livingStep1(dto.getLivingStep1())
                  .contractDocument(dto.getContractDocument())
                  .build();
      }
}
