package org.scoula.domain.home.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.scoula.domain.home.dto.HomeCreateRequestDto;
import org.scoula.domain.home.dto.HomeUpdateRequestDto;
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
      private LocalDate buildDate;
      private Integer floor;
      private Integer buildingTotalFloors;
      private HomeDirection homeDirection;
      private Integer bathroomCount;
      private Boolean isPet;
      private LocalDate moveInDate;
      private Boolean isParkingAvailable;

      // 이미지
      private List<String> imageUrls;
      private Long imageId; // << 이 필드를 추가했습니다.
      private String imageUrl;

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
          private String itemName;
      }

      // 생성용 from (HomeCreateRequestDto)
      public static HomeRegisterVO from(Long userId, HomeCreateRequestDto dto) {

          // MaintenanceFeeDTO를 MaintenanceFeeItem으로 변환
          List<MaintenanceFeeItem> maintenanceItems = null;
          if (dto.getMaintenanceFees() != null) {
              maintenanceItems =
                      dto.getMaintenanceFees().stream()
                              .map(
                                      fee ->
                                              MaintenanceFeeItem.builder()
                                                      .maintenanceId(
                                                              fee.getMaintenanceId() != null
                                                                      ? fee.getMaintenanceId()
                                                                              .longValue()
                                                                      : null)
                                                      .fee(fee.getFee())
                                                      .build())
                              .collect(java.util.stream.Collectors.toList());
          }

          // facilityItemIds를 Long 리스트로 변환
          List<Long> facilityIds = null;
          if (dto.getFacilityItemIds() != null) {
              facilityIds =
                      dto.getFacilityItemIds().stream()
                              .map(Integer::longValue)
                              .collect(java.util.stream.Collectors.toList());
          }

          return HomeRegisterVO.builder()
                  .userId(userId)
                  // userName은 HomeCreateRequestDto에 없음
                  .addr1(dto.getAddr1())
                  .addr2(dto.getAddr2())
                  .residenceType(dto.getResidenceType())
                  .leaseType(dto.getLeaseType())
                  .depositPrice(dto.getDepositPrice())
                  .monthlyRent(dto.getMonthlyRent())
                  .maintenanceFee(dto.getMaintenanceFee())
                  .supplyArea(dto.getSupplyArea() != null ? dto.getSupplyArea() : 0f)
                  .exclusiveArea(dto.getExclusiveArea())
                  .homeFloor(dto.getHomeFloor() != null ? dto.getHomeFloor().toString() : null)
                  .roomCnt(dto.getRoomCnt())
                  .bathroomCount(dto.getBathroomCnt()) // 주의: 필드명 다름
                  .facilityItemIds(facilityIds)
                  .buildDate(dto.getBuildDate())
                  // options는 HomeCreateRequestDto에 없음
                  .isParkingAvailable(dto.getIsParking()) // 주의: 필드명 다름
                  .buildingTotalFloors(dto.getBuildingTotalFloors())
                  .isPet(dto.getIsPet())
                  // moveInDate는 HomeCreateRequestDto에 없음
                  .maintenanceItems(maintenanceItems)
                  // imageUrls는 images 필드가 MultipartFile이므로 여기서는 처리 안함
                  .build();
      }

      // 수정용 from (HomeUpdateRequestDto)
      public static HomeRegisterVO from(Long userId, HomeUpdateRequestDto dto) {
          LocalDateTime parsedBuildDate =
                  dto.getBuildDate() != null ? dto.getBuildDate().atStartOfDay() : null;

          // MaintenanceFeeDTO를 MaintenanceFeeItem으로 변환
          List<MaintenanceFeeItem> maintenanceItems = null;
          if (dto.getMaintenanceFees() != null) {
              maintenanceItems =
                      dto.getMaintenanceFees().stream()
                              .map(
                                      fee ->
                                              MaintenanceFeeItem.builder()
                                                      .maintenanceId(
                                                              fee.getMaintenanceId() != null
                                                                      ? fee.getMaintenanceId()
                                                                              .longValue()
                                                                      : null)
                                                      .fee(fee.getFee())
                                                      .build())
                              .collect(java.util.stream.Collectors.toList());
          }

          // facilityItemIds를 Long 리스트로 변환
          List<Long> facilityIds = null;
          if (dto.getFacilityItemIds() != null) {
              facilityIds =
                      dto.getFacilityItemIds().stream()
                              .map(Integer::longValue)
                              .collect(java.util.stream.Collectors.toList());
          }

          return HomeRegisterVO.builder()
                  .homeId(dto.getHomeId() != null ? dto.getHomeId().longValue() : null)
                  .userId(userId)
                  // userName은 HomeUpdateRequestDto에 없음
                  .addr1(dto.getAddr1())
                  .addr2(dto.getAddr2())
                  .residenceType(dto.getResidenceType())
                  .leaseType(dto.getLeaseType())
                  .depositPrice(dto.getDepositPrice())
                  .monthlyRent(dto.getMonthlyRent())
                  .maintenanceFee(dto.getMaintenanceFee())
                  .supplyArea(dto.getSupplyArea() != null ? dto.getSupplyArea() : 0f)
                  .exclusiveArea(dto.getExclusiveArea())
                  .homeFloor(dto.getHomeFloor() != null ? dto.getHomeFloor().toString() : null)
                  .roomCnt(dto.getRoomCnt())
                  .bathroomCount(dto.getBathroomCnt()) // 주의: 필드명 다름
                  .homeDirection(dto.getHomeDirection())
                  .imageUrls(dto.getExistingImageUrls()) // 기존 이미지 URL 리스트
                  .facilityItemIds(facilityIds)
                  // options는 HomeUpdateRequestDto에 없음
                  .isParkingAvailable(dto.getIsParking()) // 주의: 필드명 다름
                  .buildingTotalFloors(dto.getBuildingTotalFloors())
                  .isPet(dto.getIsPet())
                  // moveInDate는 HomeUpdateRequestDto에 없음
                  .buildDate(dto.getBuildDate())
                  .maintenanceItems(maintenanceItems)
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
