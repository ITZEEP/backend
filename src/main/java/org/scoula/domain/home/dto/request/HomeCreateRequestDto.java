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

      private String addr1; // 시/도 + 구/군
      private String addr2; // 상세 주소
      private String userName; // 유저 실명

      private ResidenceType residenceType; // 예: 오피스텔, 투룸
      private LeaseType leaseType; // 전세, 월세

      private Integer depositPrice; // 보증금
      private Integer monthlyRent; // 월세
      private Integer maintenanceFee; // 관리비
      private String itemName;

      private Float supplyArea; // 공급면적
      private Float exclusiveArea; // 전용면적

      private String homeFloor; // 층 정보 (예: "5층 / 15층" 등)

      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
      private LocalDate buildDate;

      private Integer buildingTotalFloors;
      private Boolean isPet;
      private Boolean isParkingAvailable;

      private Integer roomCnt; // 방 개수
      private Integer bathroomCount; // 욕실 개수
      private String homeDirection; // 남향, 북향 등

      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
      private LocalDate moveInDate;

      private List<String> options; // 옵션 (가전 등)
      private List<Long> facilityItemIds; // 시설 ID 리스트

      // ✅ 관리비 항목 리스트
      private List<HomeRegisterVO.MaintenanceFeeItem> maintenanceFeeItems;

      // ✅ 이미지 관련: 업로드용 파일과 저장용 URL 분리
      private List<MultipartFile> images;
      private List<MultipartFile> imageFiles;
      private List<String> imageUrls; // DB 저장용
}
