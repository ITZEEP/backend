package org.scoula.domain.contract.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 암호 제출 DTO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordSubmitDTO {
      private String userRole; // "owner" or "buyer"
      private String password;
      private Long submittedAt;
}
