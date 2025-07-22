package org.scoula.domain.fraud.dto.common;

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
@ApiModel(description = "상세 분석 결과")
public class DetailedAnalysisDto {

      @ApiModelProperty(value = "기본정보 분석")
      private BasicInfoAnalysis basicInfo;

      @ApiModelProperty(value = "건축물 정보 분석")
      private BuildingInfoAnalysis buildingInfo;

      @ApiModelProperty(value = "권리 관계 분석")
      private RightsAnalysis rightsInfo;

      @ApiModelProperty(value = "법적 위험 분석")
      private LegalRiskAnalysis legalRisk;

      @Getter
      @Setter
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      @ApiModel(description = "기본정보 분석")
      public static class BasicInfoAnalysis {

          @ApiModelProperty(value = "소유자 정보 일치 여부 AI 답변", example = "등기부등본의 소유자와 임대인 정보가 일치합니다.")
          private String ownerInfoMatch;

          @ApiModelProperty(value = "주소 일치 여부 AI 답변", example = "등기부등본과 건축물대장의 주소가 정확히 일치합니다.")
          private String addressMatch;
      }

      @Getter
      @Setter
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      @ApiModel(description = "건축물 정보 분석")
      public static class BuildingInfoAnalysis {

          @ApiModelProperty(value = "건물 용도 확인 AI 답변", example = "건축물대장상 용도는 아파트로 주거용으로 적합합니다.")
          private String buildingPurposeCheck;

          @ApiModelProperty(
                  value = "층수&연면적 정보 AI 답변",
                  example = "15층 건물 중 10층에 위치하며, 전용면적 84㎡로 표기되어 있습니다.")
          private String floorAreaInfo;
      }

      @Getter
      @Setter
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      @ApiModel(description = "권리 관계 분석")
      public static class RightsAnalysis {

          @ApiModelProperty(value = "근저당권 여부 AI 답변", example = "OO은행 앞 근저당 5억원이 설정되어 있어 주의가 필요합니다.")
          private String mortgageStatus;

          @ApiModelProperty(value = "실거래가 비율 AI 답변", example = "시세 대비 95% 수준으로 적정한 가격대입니다.")
          private String realPriceRatio;
      }

      @Getter
      @Setter
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      @ApiModel(description = "법적 위험 분석")
      public static class LegalRiskAnalysis {

          @ApiModelProperty(
                  value = "가압류&경매&소송기록 AI 답변",
                  example = "현재 진행 중인 법적 분쟁이나 강제집행 절차가 없어 안전합니다.")
          private String legalProceedingsStatus;

          @ApiModelProperty(value = "위반 건축물 여부 AI 답변", example = "적법하게 건축된 건물로 위반사항이 없습니다.")
          private String violationBuildingStatus;
      }
}
