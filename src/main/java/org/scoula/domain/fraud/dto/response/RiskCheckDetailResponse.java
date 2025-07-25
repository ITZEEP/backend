package org.scoula.domain.fraud.dto.response;

import java.util.List;

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
@ApiModel(description = "사기 위험도 분석 결과 상세 조회 응답")
public class RiskCheckDetailResponse {

      @ApiModelProperty(value = "위험도 체크 ID", example = "1")
      private Long riskCheckId;

      @ApiModelProperty(value = "사용자 ID", example = "1")
      private Long userId;

      @ApiModelProperty(value = "매물 ID", example = "1")
      private Long homeId;

      @ApiModelProperty(value = "종합 위험도 타입", example = "SAFE")
      private RiskType riskType;

      // 추가 필드들
      @ApiModelProperty(value = "등기부등본 파일 URL")
      private String registryFileUrl;

      @ApiModelProperty(value = "건축물대장 파일 URL")
      private String buildingFileUrl;

      @ApiModelProperty(value = "주소")
      private String address;

      @ApiModelProperty(value = "상세 주소")
      private String detailAddress;

      @ApiModelProperty(value = "거주 유형", example = "월세")
      private String residenceType;

      @ApiModelProperty(value = "매물 이미지 URL")
      private String imageUrl;

      @ApiModelProperty(value = "면적")
      private Float size;

      @ApiModelProperty(value = "방 개수")
      private Integer roomNum;

      @ApiModelProperty(value = "화장실 개수")
      private Integer toiletNum;

      @ApiModelProperty(value = "완공 날짜")
      private java.time.LocalDate completionDate;

      @ApiModelProperty(value = "월세", example = "50")
      private Integer monthlyRent;

      @ApiModelProperty(value = "보증금/전세가", example = "5000")
      private Integer depositPrice;

      @ApiModelProperty(value = "관리비")
      private Integer managePrice;

      @ApiModelProperty(value = "상세 분석 그룹들")
      private List<DetailGroup> detailGroups;

      @ApiModelProperty(value = "체크 날짜")
      private java.time.LocalDateTime checkedAt;

      @ApiModelProperty(value = "거래 타입", example = "전세")
      private String transactionType;

      @ApiModelProperty(value = "임대 타입", example = "JEONSE")
      private String leaseType;

      @Getter
      @Setter
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      @ApiModel(description = "상세 분석 그룹")
      public static class DetailGroup {
          @ApiModelProperty(value = "그룹 제목", example = "갑기본정보")
          private String title;

          @ApiModelProperty(value = "그룹 내 항목들")
          private List<DetailItem> items;
      }

      @Getter
      @Setter
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      @ApiModel(description = "상세 분석 항목")
      public static class DetailItem {
          @ApiModelProperty(value = "항목 제목", example = "소유")
          private String title;

          @ApiModelProperty(value = "내용", example = "등기부등본의 소유자가 임대인 정보와 일치합니다.")
          private String content;
      }
}
