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
@ApiModel(description = "건물 정보")
public class BuildingInfoDto {

      @ApiModelProperty(value = "주소", required = true, example = "서울특별시 강남구 테헤란로 123")
      private String address;

      @ApiModelProperty(value = "상세주소", example = "101동 1503호")
      private String detailAddress;

      @ApiModelProperty(value = "건물 유형", example = "아파트")
      private String buildingType;

      @ApiModelProperty(value = "거래 유형", example = "전세")
      private String transactionType;

      @ApiModelProperty(value = "가격 (원)", example = "500000000")
      private Long price;
}
