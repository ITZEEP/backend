package org.scoula.domain.chat.document;

import java.util.List;

import org.scoula.domain.chat.dto.ContentDataDto;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "SPECIAL_CONTRACT_FIX")
public class SpecialContractFixDocument {
      @Id private String id;
      private Long contractChatId;
      private Long order;
      private Long round;
      private Boolean isPassed;
      private List<ContentDataDto> prevData;
      private ContentDataDto recentData;
}
