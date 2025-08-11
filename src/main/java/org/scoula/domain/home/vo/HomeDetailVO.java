package org.scoula.domain.home.vo;

import java.time.LocalDate;

import org.scoula.domain.home.enums.HomeDirection;

import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HomeDetailVO {
      private Integer homeDetailId;
      private Integer homeId;
      private LocalDate buildDate;
      private Integer homeFloor;
      private Integer buildingTotalFloors;
      private HomeDirection homeDirection;
      private Integer bathroomCnt;
      private Boolean isPet;
      private Boolean isParking;
}
