package org.scoula.domain.home.vo;

import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HomeImageVO {
      private Integer imageId;
      private Integer homeId;
      private String ImageUrl;
}
