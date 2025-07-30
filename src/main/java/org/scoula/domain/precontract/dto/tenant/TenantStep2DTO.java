package org.scoula.domain.precontract.dto.tenant;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(description = "임차인 계약전 step2")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantStep2DTO {
      @ApiModelProperty(value = "주요설비 보수 필요 여부", example = "false", allowableValues = "true,false")
      private Boolean facilityRepairNeeded;

      @ApiModelProperty(
              value = "입주 전 도배, 장판, 청소 필요 여부",
              example = "true",
              allowableValues = "true,false")
      private Boolean interiorCleaningNeeded;

      @ApiModelProperty(
              value = "벽걸이, tv, 에어컨 설치 계획",
              example = "true",
              allowableValues = "true,false")
      private Boolean applianceInstallationPlan;

      @ApiModelProperty(value = "반려동물 여부", example = "true", allowableValues = "true,false")
      private Boolean hasPet;

      @ApiModelProperty(value = "반려동물 종", example = "강아지")
      private String petInfo;

      @ApiModelProperty(value = "반려동물 수", example = "1")
      private Long petCount;
}
