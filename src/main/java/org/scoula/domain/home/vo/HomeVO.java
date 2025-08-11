package org.scoula.domain.home.vo;

import java.time.LocalDate;

import org.scoula.domain.home.enums.HomeStatus;
import org.scoula.domain.home.enums.LeaseType;
import org.scoula.domain.home.enums.ResidenceType;

import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HomeVO {
      private Integer homeId;
      private Integer userId;
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
      private LocalDate createdAt;
      private LocalDate updatedAt;
      private String userName;
      private Float exclusiveArea;
}
