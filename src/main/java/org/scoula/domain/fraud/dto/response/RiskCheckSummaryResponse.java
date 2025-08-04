package org.scoula.domain.fraud.dto.response;

import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 다른 도메인에 제공할 사기 위험도 요약 정보 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "사기 위험도 체크 요약 정보")
public class RiskCheckSummaryResponse {

      @ApiModelProperty(value = "위험도 체크 ID", example = "1")
      private Long riskCheckId;

      @ApiModelProperty(value = "위험도 등급", example = "SAFE", allowableValues = "SAFE, WARN, DANGER")
      private String riskType;

      @ApiModelProperty(value = "분석 상세 내용 그룹")
      private List<DetailGroup> detailGroups;

      @Getter
      @Setter
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      @ApiModel(description = "분석 상세 그룹")
      public static class DetailGroup {

          @ApiModelProperty(value = "그룹 제목", example = "건축 관련")
          private String title;

          @ApiModelProperty(value = "세부 항목들")
          private List<DetailItem> items;
      }

      @Getter
      @Setter
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      @ApiModel(description = "분석 상세 항목")
      public static class DetailItem {

          @ApiModelProperty(value = "항목 제목", example = "건축물 적법성 확인 필요")
          private String title;

          @ApiModelProperty(value = "상세 내용", example = "건축물대장에 따르면 위반건축물은 아니라고 기재되어 있습니다.")
          private String content;
      }
}
