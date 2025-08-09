package org.scoula.domain.home.dto.request;

import java.time.LocalDate;
import java.util.List;

import org.scoula.domain.home.enums.LeaseType;
import org.scoula.domain.home.enums.ResidenceType;
import org.scoula.domain.home.vo.HomeRegisterVO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeCreateRequestDto {

      private String addr1;
      private String addr2;
      private String userName;

      private ResidenceType residenceType;
      private LeaseType leaseType;

      private Integer depositPrice;
      private Integer monthlyRent;
      private Integer maintenanceFee;

      private Float supplyArea;
      private Float exclusiveArea;

      private String homeFloor; // String -> Integer로 변경
      private Integer buildingTotalFloors;

      private Integer roomCnt;
      private Integer bathroomCount;
      private String homeDirection;

      private Boolean isPet;
      private Boolean isParkingAvailable;

      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
      private LocalDate buildDate;

      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
      private LocalDate moveInDate;

      // 이미지 파일 목록
      private List<MultipartFile> imageFiles;

      // 관리비 항목 목록
      private List<HomeRegisterVO.MaintenanceFeeItem> maintenanceFeeItems;

      // 시설 항목 ID 목록
      private List<Long> facilityItemIds;
}
