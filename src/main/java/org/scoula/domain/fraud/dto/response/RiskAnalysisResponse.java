package org.scoula.domain.fraud.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import org.scoula.domain.fraud.dto.common.BuildingInfoDto;
import org.scoula.domain.fraud.enums.RiskType;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@ApiModel(description = "사기 위험도 분석 응답")
public class RiskAnalysisResponse {

      @ApiModelProperty(value = "위험도 체크 ID", example = "1")
      private Long riskCheckId;

      @ApiModelProperty(value = "건물 정보")
      private BuildingInfoDto buildingInfo;

      @ApiModelProperty(value = "종합 위험도 타입", example = "SAFE")
      private RiskType riskType;

      @ApiModelProperty(value = "분석 일시")
      private LocalDateTime analyzedAt;

      @ApiModelProperty(value = "상세 분석 그룹들")
      private List<RiskCheckDetailResponse.DetailGroup> detailGroups;
}
