package org.scoula.domain.home.dto;

import org.scoula.domain.home.enums.HomeDirection;
import org.scoula.domain.home.enums.HomeStatus;
import org.scoula.domain.home.enums.LeaseType;
import org.scoula.domain.home.enums.ResidenceType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeSearchDTO {

      private ResidenceType residenceType;
      private LeaseType leaseType;
      private HomeStatus homeStatus;
      private HomeDirection homeDirection;

      private Integer minDepositPrice;
      private Integer maxDepositPrice;
      private Integer minMonthlyRent;
      private Integer maxMonthlyRent;
      private Integer maxMaintenanceFee;

      private Float minSupplyArea;
      private Float maxSupplyArea;
      private Integer minRoomCnt;
      private Integer maxRoomCnt;

      // 층수 범위
      private Integer minFloor;
      private Integer maxFloor;

      // 기타 조건
      private Boolean isPet;
      private Boolean isParking;
      private String addr1; // 주소 검색

      // 페이징
      private Integer page = 1;
      private Integer size = 21;

      // 정렬
      private String sortBy = "createdAt"; // createdAt, price, viewCnt, likeCnt
      private String sortDirection = "DESC"; // ASC, DESC

      // 페이징 계산용 메서드
      public int getOffset() {
          return (page - 1) * size;
      }

      public int getLimit() {
          return size;
      }
}
