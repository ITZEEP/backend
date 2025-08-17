package org.scoula.domain.home.dto;

import java.time.LocalDate;
import java.util.List;

import javax.validation.constraints.*;

import org.scoula.domain.home.enums.HomeDirection;
import org.scoula.domain.home.enums.LeaseType;
import org.scoula.domain.home.enums.ResidenceType;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(description = "매물 수정 요청 DTO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeUpdateRequestDto {

      @ApiModelProperty(value = "매물 ID", example = "123", notes = "URL 경로에서 자동으로 설정됨")
      private Integer homeId;

      @ApiModelProperty(value = "시/도 및 시/군/구", example = "서울특별시 강남구", required = true)
      private String addr1;

      @ApiModelProperty(value = "상세 주소", example = "테헤란로 456 빌라 201호")
      private String addr2;

      @ApiModelProperty(
              value = "거주 유형",
              example = "VILLA",
              required = true,
              allowableValues = "APARTMENT, VILLA, ONEROOM, OFFICETEL, HOUSE")
      private ResidenceType residenceType;

      @ApiModelProperty(
              value = "임대 유형",
              example = "JEONSE",
              required = true,
              allowableValues = "JEONSE, WOLSE")
      private LeaseType leaseType;

      @ApiModelProperty(value = "보증금 (원)", example = "300000000", required = true)
      private Integer depositPrice;

      @ApiModelProperty(value = "월세 (원)", example = "0", notes = "전세인 경우 0")
      private Integer monthlyRent;

      @ApiModelProperty(value = "관리비 (원)", example = "100000")
      private Integer maintenanceFee;

      @ApiModelProperty(value = "방 개수", example = "2")
      private Integer roomCnt;

      @ApiModelProperty(value = "공급 면적 (㎡)", example = "59.7")
      private Float supplyArea;

      @ApiModelProperty(value = "전용 면적 (㎡)", example = "48.3")
      private Float exclusiveArea;

      @ApiModelProperty(value = "준공일", example = "2018-07-20")
      private LocalDate buildDate;

      @ApiModelProperty(value = "해당 층수", example = "2")
      private Integer homeFloor;

      @ApiModelProperty(value = "건물 총 층수", example = "5")
      private Integer buildingTotalFloors;

      @ApiModelProperty(
              value = "집 방향",
              example = "SE",
              allowableValues = "E, W, S, N, SE, SW, NE, NW")
      private HomeDirection homeDirection;

      @ApiModelProperty(value = "욕실 개수", example = "1")
      private Integer bathroomCnt;

      @ApiModelProperty(value = "반려동물 가능 여부", example = "false")
      private Boolean isPet;

      @ApiModelProperty(value = "주차 가능 여부", example = "true")
      private Boolean isParking;

      @ApiModelProperty(value = "편의시설 ID 목록", example = "[2, 4, 6, 7, 9]")
      private List<Integer> facilityItemIds;

      @ApiModelProperty(value = "관리비 항목 목록")
      private List<MaintenanceFeeDTO> maintenanceFees;

      @ApiModelProperty(
              value = "삭제할 이미지 ID 목록",
              example = "[1, 3]",
              notes = "삭제하려는 기존 이미지의 ID를 입력하세요")
      private List<Integer> deleteImageIds;

      @ApiModelProperty(
              value = "기존 이미지 URL 목록",
              notes = "읽기 전용, 현재 등록된 이미지를 표시하기 위한 용도",
              readOnly = true)
      private List<String> existingImageUrls;

      @ApiModelProperty(hidden = true)
      private List<MultipartFile> newImages;

      @ApiModel(description = "관리비 항목 DTO")
      @Data
      @NoArgsConstructor
      @AllArgsConstructor
      @Builder
      public static class MaintenanceFeeDTO {
          @ApiModelProperty(
                  value = "관리비 항목 ID",
                  example = "2",
                  notes = "1: 전기세, 2: 수도세, 3: 가스비, 4: 인터넷, 5: TV")
          private Integer maintenanceId;

          @ApiModelProperty(value = "관리비 금액 (원)", example = "30000")
          private Integer fee;
      }
}
