package org.scoula.domain.home.dto.request;

import java.time.LocalDate;
import java.util.List;

import org.scoula.domain.home.enums.LeaseType;
import org.scoula.domain.home.enums.ResidenceType;

import lombok.*;
import lombok.Getter;
import lombok.Setter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class HomeUpdateRequestDto {
      private Long homeId;
      private String userName;

      private String addr1;
      private String addr2;

      private ResidenceType residenceType;
      private LeaseType leaseType;

      private Integer depositPrice;
      private Integer monthlyRent;
      private Integer maintenanceFee;

      private Float supplyArea;
      private Float exclusiveArea;

      private String homefloor;
      private LocalDate buildDate;
      private Integer buildingTotalFloors;
      private Boolean isPet;
      private Boolean isParkingAvailable;

      private Integer roomCnt;
      private Integer bathroomCount;
      private String homeDirection;
      private LocalDate moveInDate;

      private List<String> imageUrls;

      private List<String> options;
      private List<Long> facilityItemIds;
}
