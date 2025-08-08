package org.scoula.domain.precontract.dto.owner;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(description = "임대인 계약전 기본 정보 반환 DTO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerInitRespDTO {
      private String rentType;

      public static OwnerInitRespDTO toResp(String rentType) {
          return OwnerInitRespDTO.builder().rentType(rentType).build();
      }
}
