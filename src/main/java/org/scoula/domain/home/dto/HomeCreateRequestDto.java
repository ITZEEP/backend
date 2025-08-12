package org.scoula.domain.home.dto;

import java.time.LocalDate;
import java.util.List;

import javax.validation.constraints.*;

import org.scoula.domain.home.enums.HomeDirection;
import org.scoula.domain.home.enums.LeaseType;
import org.scoula.domain.home.enums.ResidenceType;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(description = "매물 등록 요청 DTO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeCreateRequestDto {

      @ApiModelProperty(value = "시/도 및 시/군/구", example = "서울특별시 강남구", required = true)
      private String addr1;

      @ApiModelProperty(value = "상세 주소", example = "테헤란로 123 아파트 101동 501호")
      private String addr2;

      @ApiModelProperty(
              value = "거주 유형",
              example = "APARTMENT",
              required = true,
              allowableValues = "APARTMENT, VILLA, ONEROOM, OFFICETEL, HOUSE")
      private ResidenceType residenceType;

      @ApiModelProperty(
              value = "임대 유형",
              example = "WOLSE",
              required = true,
              allowableValues = "JEONSE, WOLSE")
      private LeaseType leaseType;

      @ApiModelProperty(value = "보증금 (원)", example = "50000000", required = true)
      private Integer depositPrice;

      @ApiModelProperty(value = "월세 (원)", example = "1000000", notes = "전세인 경우 0")
      private Integer monthlyRent;

      @ApiModelProperty(value = "관리비 (원)", example = "150000")
      private Integer maintenanceFee;

      @ApiModelProperty(value = "방 개수", example = "3")
      private Integer roomCnt;

      @ApiModelProperty(value = "공급 면적 (㎡)", example = "84.5")
      private Float supplyArea;

      @ApiModelProperty(value = "전용 면적 (㎡)", example = "59.8")
      private Float exclusiveArea;

      @ApiModelProperty(value = "준공일", example = "2020-03-15")
      private LocalDate buildDate;

      @ApiModelProperty(value = "해당 층수", example = "5")
      private Integer homeFloor;

      @ApiModelProperty(value = "건물 총 층수", example = "15")
      private Integer buildingTotalFloors;

      @ApiModelProperty(value = "집 방향", example = "S", allowableValues = "E, W, S, N, SE, SW, NE, NW")
      private HomeDirection homeDirection;

      @ApiModelProperty(value = "욕실 개수", example = "2")
      private Integer bathroomCnt;

      @ApiModelProperty(value = "반려동물 가능 여부", example = "true")
      private Boolean isPet;

      @ApiModelProperty(value = "주차 가능 여부", example = "true")
      private Boolean isParking;

      @ApiModelProperty(value = "편의시설 ID 목록", example = "[1, 2, 3, 5, 8]")
      private List<Integer> facilityItemIds;

      @ApiModelProperty(value = "관리비 항목 목록")
      private List<MaintenanceFeeDTO> maintenanceFees;

      @ApiModel(description = "관리비 항목 DTO")
      @Data
      @NoArgsConstructor
      @AllArgsConstructor
      @Builder
      public static class MaintenanceFeeDTO {
          @ApiModelProperty(
                  value = "관리비 항목 ID",
                  example = "1",
                  notes = "1: 전기세, 2: 수도세, 3: 가스비, 4: 인터넷, 5: TV")
          private Integer maintenanceId;

          @ApiModelProperty(value = "관리비 금액 (원)", example = "50000")
          private Integer fee;
      }
}
