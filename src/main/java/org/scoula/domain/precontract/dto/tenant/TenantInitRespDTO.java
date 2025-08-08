package org.scoula.domain.precontract.dto.tenant;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(description = "임차인 계약전 기본 정보 반환 DTO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantInitRespDTO {

      private String rentType;
      private boolean hasPet;
      private boolean hasParking;

      public static TenantInitRespDTO toResp(String rentType, boolean hasPet, boolean hasParking) {
          return TenantInitRespDTO.builder()
                  .rentType(rentType)
                  .hasPet(hasPet)
                  .hasParking(hasParking)
                  .build();
      }
}
