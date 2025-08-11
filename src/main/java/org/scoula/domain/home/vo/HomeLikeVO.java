package org.scoula.domain.home.vo;

import java.time.LocalDate;

import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HomeLikeVO {
      private Integer userId;
      private Integer homeId;
      private LocalDate likedAt;
}
