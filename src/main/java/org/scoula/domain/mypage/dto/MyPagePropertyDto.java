package org.scoula.domain.mypage.dto;

import org.scoula.domain.home.enums.HomeStatus;
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
@ApiModel(description = "마이페이지 매물 정보")
public class MyPagePropertyDto {
      @ApiModelProperty(value = "매물 ID", example = "1")
      private Long propertyId;

      @ApiModelProperty(value = "매물 주소", example = "서울시 강남구 역삼동 123-45")
      private String address;

      @ApiModelProperty(value = "건물 유형", example = "APARTMENT")
      private ResidenceType buildingType;

      @ApiModelProperty(value = "매물 상태", example = "AVAILABLE")
      private HomeStatus status;

      @ApiModelProperty(value = "조회수", example = "150")
      private Integer viewCount;

      @ApiModelProperty(value = "좋아요 수", example = "25")
      private Integer likeCount;

      @ApiModelProperty(value = "대표 이미지 URL", example = "https://example.com/images/property1.jpg")
      private String imageUrl;

      @ApiModelProperty(value = "임대 유형", example = "JEONSE")
      private LeaseType leaseType;
}
