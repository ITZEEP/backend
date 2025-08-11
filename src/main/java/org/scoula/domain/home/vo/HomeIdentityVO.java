package org.scoula.domain.home.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class HomeIdentityVO {
      private Long homeIdentityId;
      private Long homeId;
      private Long userId;
      private String name;
      private LocalDate birthDate;
      private LocalDateTime identityVerifiedAt;
}
