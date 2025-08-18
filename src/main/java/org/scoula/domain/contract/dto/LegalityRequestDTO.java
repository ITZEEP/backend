package org.scoula.domain.contract.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalityRequestDTO {
      private String legalBasis;
      private Long requestId;
      private String createdAt;
}
