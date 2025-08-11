package org.scoula.domain.home.dto;

import java.time.LocalDate;
import java.util.List;

import javax.validation.constraints.*;

import org.scoula.domain.home.enums.HomeDirection;
import org.scoula.domain.home.enums.LeaseType;
import org.scoula.domain.home.enums.ResidenceType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeCreateDTO {

      private String addr1;

      private String addr2;

      private ResidenceType residenceType;

      private LeaseType leaseType;

      private Integer depositPrice;

      private Integer monthlyRent;

      private Integer maintenaceFee;

      private Integer roomCnt;

      private Float supplyArea;

      private Float exclusiveArea;

      private LocalDate buildDate;

      private Integer homeFloor;

      private Integer buildingTotalFloors;

      private HomeDirection homeDirection;

      private Integer bathroomCnt;

      private Boolean isPet;
      private Boolean isParking;

      private List<Integer> facilityItemIds;

      private List<MaintenanceFeeDTO> maintenanceFees;

      private List<String> imageUrls;

      @Data
      @NoArgsConstructor
      @AllArgsConstructor
      @Builder
      public static class MaintenanceFeeDTO {
          private Integer maintenanceId;
          private Integer fee;
      }
}
