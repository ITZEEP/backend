package org.scoula.domain.home.dto;

import java.time.LocalDate;
import java.util.List;

import org.scoula.domain.home.enums.HomeDirection;
import org.scoula.domain.home.enums.HomeStatus;
import org.scoula.domain.home.enums.LeaseType;
import org.scoula.domain.home.enums.ResidenceType;
import org.scoula.domain.home.vo.FacilityItem;
import org.scoula.domain.home.vo.HomeMaintenanceFeeVO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeResponseDTO {

      private Integer homeId;
      private Integer userId;
      private String userName;

      private String addr1;
      private String addr2;

      private ResidenceType residenceType;
      private LeaseType leaseType;
      private Integer depositPrice;
      private Integer monthlyRent;
      private Integer maintenaceFee;
      private HomeStatus homeStatus;

      private Integer viewCnt;
      private Integer likeCnt;
      private Integer chatCnt;

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

      private List<FacilityItem> facilities;

      private List<HomeMaintenanceFeeVO> maintenanceFees;

      private List<String> imageUrls;
      private LocalDate createdAt;
      private LocalDate updatedAt;
}
