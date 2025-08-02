package org.scoula.domain.fraud.dto.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 오늘 분석한 사기 위험도 조회 응답 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "오늘 분석한 사기 위험도 조회 응답")
public class TodayRiskCheckResponse {

      @ApiModelProperty(value = "오늘 분석한 결과 존재 여부", example = "true")
      private boolean hasAnalysis;

      @ApiModelProperty(value = "사기 위험도 요약 정보 (분석 결과가 있는 경우에만 존재)")
      private RiskCheckSummaryResponse summary;

      @ApiModelProperty(value = "메시지", example = "오늘 분석한 결과가 있습니다.")
      private String message;

      public static TodayRiskCheckResponse withAnalysis(RiskCheckSummaryResponse summary) {
          return TodayRiskCheckResponse.builder()
                  .hasAnalysis(true)
                  .summary(summary)
                  .message("오늘 분석한 결과가 있습니다.")
                  .build();
      }

      public static TodayRiskCheckResponse noAnalysis() {
          return TodayRiskCheckResponse.builder()
                  .hasAnalysis(false)
                  .summary(null)
                  .message("오늘 분석한 결과가 없습니다.")
                  .build();
      }
}
