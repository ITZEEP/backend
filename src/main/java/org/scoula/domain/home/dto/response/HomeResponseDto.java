package org.scoula.domain.home.dto.response;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
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
      private Long homeDetailId;
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
      private Float exclusiveArea;

      private String homeFloor;
      private Integer buildingTotalFloors;

      private Integer roomCnt;
      private Integer bathroomCount;
      private String homeDirection;

      private Boolean isPet;
      private Boolean isParkingAvailable;

      private LocalDate buildDate;
      private LocalDate moveInDate;

      private Integer viewCnt;
      private Integer chatCnt;
      private Integer likeCnt;

      private String imageUrl; // 대표 이미지 URL 하나
      private Long imageId;

      private List<String> options;
      private List<Long> facilityItemIds;

      private String createdAt;

      public static HomeResponseDto from(HomeRegisterVO vo) {
          String createdAtStr = null;
          if (vo.getCreatedAt() != null) {
              createdAtStr =
                      vo.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
          }

          // 대표 이미지 URL: imageUrls 리스트 중 첫 번째, 없으면 null
          String mainImageUrl =
                  (vo.getImageUrls() != null && !vo.getImageUrls().isEmpty())
                          ? vo.getImageUrls().get(0)
                          : null;

          return HomeResponseDto.builder()
                  .homeId(vo.getHomeId())
                  .userId(vo.getUserId())
                  .userName(vo.getUserName())
                  .homeDetailId(vo.getHomeDetailId())
                  .addr1(vo.getAddr1())
                  .addr2(vo.getAddr2())
                  .residenceType(vo.getResidenceType())
                  .leaseType(vo.getLeaseType())
                  .depositPrice(vo.getDepositPrice())
                  .monthlyRent(vo.getMonthlyRent())
                  .maintenanceFee(vo.getMaintenanceFee())
                  .supplyArea(vo.getSupplyArea())
                  .exclusiveArea(vo.getExclusiveArea())
                  .homeFloor(vo.getHomeFloor() != null ? vo.getHomeFloor() : "")
                  .buildingTotalFloors(vo.getBuildingTotalFloors())
                  .roomCnt(vo.getRoomCnt())
                  .likeCnt(vo.getLikeCnt())
                  .viewCnt(vo.getViewCnt())
                  .chatCnt(vo.getChatCnt())
                  .bathroomCount(vo.getBathroomCount())
                  .homeDirection(vo.getHomeDirection() != null ? vo.getHomeDirection().name() : null)
                  .isPet(vo.getIsPet())
                  .isParkingAvailable(vo.getIsParkingAvailable())
                  .buildDate(vo.getBuildDate() != null ? vo.getBuildDate().toLocalDate() : null)
                  .moveInDate(vo.getMoveInDate())
                  .imageUrl(vo.getImageUrl()) // <-- findHomes 쿼리에서 가져온 단일 imageUrl 사용
                  .imageId(vo.getImageId()) // <-- findHomes 쿼리에서 가져온 imageId 사용
                  .options(vo.getOptions() != null ? vo.getOptions() : Collections.emptyList())
                  .facilityItemIds(
                          vo.getFacilityItemIds() != null
                                  ? vo.getFacilityItemIds()
                                  : Collections.emptyList())
                  .createdAt(createdAtStr)
                  .build();
      }
}
