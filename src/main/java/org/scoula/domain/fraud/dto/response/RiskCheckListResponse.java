package org.scoula.domain.fraud.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

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
@ApiModel(description = "사기 위험도 분석 기록 목록 응답")
public class RiskCheckListResponse {

      @ApiModelProperty(value = "위험도 체크 ID", example = "1")
      private Long riskCheckId;

      @ApiModelProperty(value = "분석 일시", example = "2024-01-15T10:30:00")
      @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
      private LocalDateTime checkedAt;

      @ApiModelProperty(value = "매물 주소", example = "서울특별시 강남구 테헤란로 123")
      private String address;

      @ApiModelProperty(value = "매물 상세 주소", example = "101동 1503호")
      private String detailAddress;

      @ApiModelProperty(value = "거주 유형", example = "월세")
      private String residenceType;

      @ApiModelProperty(value = "매물 이미지 URL", example = "https://example.com/image.jpg")
      private String imageUrl;
}
