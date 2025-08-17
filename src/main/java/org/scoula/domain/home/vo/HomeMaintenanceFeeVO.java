package org.scoula.domain.home.vo;

import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HomeMaintenanceFeeVO {
      private Integer homeId;
      private Integer maintenanceId;
      private Integer fee;
}
