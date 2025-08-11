package org.scoula.domain.home.vo;

import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FacilityItem {
      private Integer itemId;
      private Integer categoryId;
      private String itemName;
}
