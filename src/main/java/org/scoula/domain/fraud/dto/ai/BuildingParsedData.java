package org.scoula.domain.fraud.dto.ai;

import java.time.LocalDate;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildingParsedData {
      private String siteLocation;
      private String roadAddress;
      private Double totalFloorArea;
      private String purpose;
      private Integer floorNumber;
      private LocalDate approvalDate;
      private Boolean isViolationBuilding;
}
