package org.scoula.domain.fraud.dto.response;

import java.time.LocalDateTime;

import org.scoula.domain.fraud.enums.RiskType;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

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
      @JsonProperty("riskckId")
      private Long riskCheckId;

      @ApiModelProperty(value = "매물 ID", example = "10")
      private Long homeId;

      @ApiModelProperty(value = "매물 주소 (도로명)", example = "서울특별시 강남구 테헤란로 123")
      @JsonProperty("addr1")
      private String addr1;

      @ApiModelProperty(value = "매물 상세 주소", example = "101동 1503호")
      @JsonProperty("addr2")
      private String addr2;

      @ApiModelProperty(value = "위험도 타입", example = "SAFE")
      private RiskType riskType;

      @ApiModelProperty(value = "분석 일시", example = "2024-01-15T10:30:00")
      @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
      @JsonProperty("checkedAt")
      private LocalDateTime analyzedAt;

      @ApiModelProperty(value = "등기부등본 파일 존재 여부", example = "true")
      private Boolean hasRegistryFile;

      @ApiModelProperty(value = "건축물대장 파일 존재 여부", example = "true")
      private Boolean hasBuildingFile;

      @ApiModelProperty(value = "매물 상태", example = "AVAILABLE")
      private String homeStatus;

      @ApiModelProperty(value = "임대 유형", example = "JEONSE")
      private String leaseType;

      @ApiModelProperty(value = "보증금", example = "500000000")
      private Integer depositPrice;

      @ApiModelProperty(value = "월세", example = "0")
      private Integer monthlyRent;
}
