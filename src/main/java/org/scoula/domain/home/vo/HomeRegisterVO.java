package org.scoula.domain.home.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.scoula.domain.home.dto.request.HomeCreateRequestDto;
import org.scoula.domain.home.dto.request.HomeUpdateRequestDto;
import org.scoula.domain.home.enums.HomeDirection;
import org.scoula.domain.home.enums.HomeStatus;
import org.scoula.domain.home.enums.LeaseType;
import org.scoula.domain.home.enums.ResidenceType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeRegisterVO {

      // 매물 기본 정보
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
      private HomeStatus homeStatus;
      private Integer viewCnt;
      private Integer likeCnt;
      private Integer chatCnt;
      private Integer reportCnt;
      private Integer roomCnt;
      private Float supplyArea;
      private Float exclusiveArea;
      private String homeFloor;
      private LocalDateTime createdAt;
      private LocalDateTime updatedAt;

      // 상세 정보
      private Long homeDetailId;
      private LocalDateTime buildDate; // VO에서는 LocalDateTime
      private Integer floor;
      private Integer buildingTotalFloors;
      private HomeDirection homeDirection;
      private Integer bathroomCount;
      private Boolean isPet;
      private LocalDate moveInDate; // VO에서는 LocalDate
      private Boolean isParkingAvailable;

      // 사진
      private List<String> imageUrls;

      // 관리비 항목
      private List<MaintenanceFeeItem> maintenanceItems;

      // 시설 항목
      private List<String> options;
      private List<Long> facilityItemIds;

      @Data
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      public static class MaintenanceFeeItem {
          private Long maintenanceId;
          private Integer fee;
      }

      // 생성용 from (HomeCreateRequestDto)
      public static HomeRegisterVO from(Long userId, HomeCreateRequestDto dto) {
          LocalDateTime parsedBuildDate = null;
          if (dto.getBuildDate() != null) {
              parsedBuildDate = dto.getBuildDate().atStartOfDay();
          }

          return HomeRegisterVO.builder()
                  .userId(userId)
                  .userName(dto.getUserName())
                  .addr1(dto.getAddr1())
                  .addr2(dto.getAddr2())
                  .residenceType(dto.getResidenceType())
                  .leaseType(dto.getLeaseType())
                  .depositPrice(dto.getDepositPrice())
                  .monthlyRent(dto.getMonthlyRent())
                  .maintenanceFee(dto.getMaintenanceFee())
                  .supplyArea(dto.getSupplyArea() != null ? dto.getSupplyArea() : 0f)
                  .exclusiveArea(dto.getExclusiveArea())
                  .homeFloor(dto.getHomeFloor())
                  .roomCnt(dto.getRoomCnt())
                  .bathroomCount(dto.getBathroomCount())
                  .facilityItemIds(dto.getFacilityItemIds())
                  .buildDate(parsedBuildDate)
                  .options(dto.getOptions())
                  .isParkingAvailable(dto.getIsParkingAvailable())
                  .buildingTotalFloors(dto.getBuildingTotalFloors())
                  .isPet(dto.getIsPet())
                  .moveInDate(dto.getMoveInDate())
                  .build();
      }

      // 수정용 from (HomeUpdateRequestDto)
      public static HomeRegisterVO from(Long userId, HomeUpdateRequestDto dto) {
          LocalDateTime parsedBuildDate = null;
          if (dto.getBuildDate() != null) {
              parsedBuildDate = dto.getBuildDate().atStartOfDay();
          }

          return HomeRegisterVO.builder()
                  .homeId(dto.getHomeId())
                  .userId(userId)
                  .userName(dto.getUserName())
                  .addr1(dto.getAddr1())
                  .addr2(dto.getAddr2())
                  .residenceType(dto.getResidenceType())
                  .leaseType(dto.getLeaseType())
                  .depositPrice(dto.getDepositPrice())
                  .monthlyRent(dto.getMonthlyRent())
                  .maintenanceFee(dto.getMaintenanceFee())
                  .supplyArea(dto.getSupplyArea())
                  .exclusiveArea(dto.getExclusiveArea())
                  .homeFloor(dto.getHomeFloor()) // 오타 fix: getHomefloor() -> getHomeFloor()
                  .roomCnt(dto.getRoomCnt())
                  .bathroomCount(dto.getBathroomCount())
                  .homeDirection(parseHomeDirection(dto.getHomeDirection()))
                  .imageUrls(dto.getImageUrls())
                  .facilityItemIds(dto.getFacilityItemIds())
                  .options(dto.getOptions())
                  .isParkingAvailable(dto.getIsParkingAvailable())
                  .buildingTotalFloors(dto.getBuildingTotalFloors())
                  .isPet(dto.getIsPet())
                  .moveInDate(dto.getMoveInDate())
                  .buildDate(parsedBuildDate)
                  .build();
      }

      private static HomeDirection parseHomeDirection(String direction) {
          try {
              return HomeDirection.valueOf(direction);
          } catch (IllegalArgumentException e) {
              return null;
          }
      }
}
