package org.scoula.domain.contract.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegalityRequestDTO {
      private String legalBasis;
      private Long requestId;
      private String createdAt;
}
