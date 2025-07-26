package org.scoula.domain.fraud.dto.response;

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
@ApiModel(description = "찜한 매물 정보 응답")
public class LikedHomeResponse {

      @ApiModelProperty(value = "매물 ID", example = "1")
      private Long homeId;

      @ApiModelProperty(value = "매물 이미지 URL", example = "https://example.com/image.jpg")
      private String imageUrl;

      @ApiModelProperty(value = "주소", example = "서울특별시 강남구 테헤란로 123")
      private String address;

      @ApiModelProperty(value = "상세 주소", example = "101동 1503호")
      private String detailAddress;

      @ApiModelProperty(value = "거주 유형", example = "월세")
      private String residenceType;

      @ApiModelProperty(value = "임대 유형", example = "WOLSE")
      private String leaseType;

      @ApiModelProperty(value = "보증금/전세가", example = "5000")
      private Integer depositPrice;

      @ApiModelProperty(value = "월세", example = "50")
      private Integer monthlyRent;
}
