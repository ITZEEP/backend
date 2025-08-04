package org.scoula.domain.home.dto.response;

import java.time.format.DateTimeFormatter;
import java.util.List;

import org.scoula.domain.home.enums.LeaseType;
import org.scoula.domain.home.enums.ResidenceType;
import org.scoula.domain.home.vo.HomeRegisterVO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeResponseDto {
      private Long homeId;
      private Long userId;
      private String userName;

      private String addr1;
      private String addr2;

      private ResidenceType residenceType;
      private LeaseType leaseType;

      private Integer depositPrice;
      private Integer monthlyRent;
      private Integer maintenanceFee;

      private Float supplyArea;

      private String homeFloor;

      private Integer roomCnt;
      private Integer bathroomCount;
      private String homeDirection;

      private List<String> imageUrls;

      private List<String> options;
      private List<Long> facilityItemIds;

      private String createdAt;

      public static HomeResponseDto from(HomeRegisterVO vo) {
          String createdAtStr = null;
          if (vo.getCreatedAt() != null) {
              createdAtStr =
                      vo.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
          }

          return HomeResponseDto.builder()
                  .homeId(vo.getHomeId())
                  .userId(vo.getUserId())
                  .userName(vo.getUserName())
                  .addr1(vo.getAddr1())
                  .addr2(vo.getAddr2())
                  .residenceType(vo.getResidenceType())
                  .leaseType(vo.getLeaseType())
                  .depositPrice(vo.getDepositPrice())
                  .monthlyRent(vo.getMonthlyRent())
                  .maintenanceFee(vo.getMaintenanceFee())
                  .supplyArea(vo.getSupplyArea())
                  .homeFloor(String.valueOf(vo.getFloor()))
                  .roomCnt(vo.getRoomCnt())
                  .bathroomCount(vo.getBathroomCount())
                  .homeDirection(vo.getHomeDirection() != null ? vo.getHomeDirection().name() : null)
                  .imageUrls(vo.getImageUrls())
                  .options(vo.getOptions())
                  .facilityItemIds(vo.getFacilityItemIds())
                  .createdAt(createdAtStr)
                  .build();
      }
}
