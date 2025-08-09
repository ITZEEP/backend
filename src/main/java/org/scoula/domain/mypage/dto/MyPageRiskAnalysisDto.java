package org.scoula.domain.mypage.dto;

import java.time.LocalDateTime;

import org.scoula.domain.fraud.enums.RiskType;
import org.scoula.domain.home.enums.LeaseType;
import org.scoula.domain.home.enums.ResidenceType;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "마이페이지 위험 분석 정보")
public class MyPageRiskAnalysisDto {
      @ApiModelProperty(value = "분석 ID", example = "1")
      private Long analysisId;

      @ApiModelProperty(value = "매물 주소", example = "서울시 강남구 역삼동 123-45")
      private String address;

      @ApiModelProperty(value = "건물 유형", example = "APARTMENT")
      private ResidenceType buildingType;

      @ApiModelProperty(value = "분석 일시", example = "2024-01-15T10:30:00")
      private LocalDateTime analysisDate;

      @ApiModelProperty(value = "위험도 타입", example = "SAFE", allowableValues = "DANGER, WARN, SAFE")
      private RiskType riskType;

      @ApiModelProperty(value = "임대 유형", example = "JEONSE")
      private LeaseType leaseType;
}
