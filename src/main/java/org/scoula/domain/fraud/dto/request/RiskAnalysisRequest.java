package org.scoula.domain.fraud.dto.request;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.scoula.domain.fraud.dto.common.BuildingDocumentDto;
import org.scoula.domain.fraud.dto.common.RegistryDocumentDto;

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
@ApiModel(description = "사기 위험도 분석 요청")
public class RiskAnalysisRequest {

      @ApiModelProperty(value = "매물 ID", example = "10")
      private Long homeId;

      @ApiModelProperty(value = "등기부등본 정보", required = true)
      @NotNull(message = "등기부등본 정보는 필수입니다")
      @Valid
      private RegistryDocumentDto registryDocument;

      @ApiModelProperty(value = "건축물대장 정보", required = true)
      @NotNull(message = "건축물대장 정보는 필수입니다")
      @Valid
      private BuildingDocumentDto buildingDocument;

      @ApiModelProperty(value = "등기부등본 파일 URL", example = "/files/registry/1234567890.pdf")
      private String registryFileUrl;

      @ApiModelProperty(value = "건축물대장 파일 URL", example = "/files/building/0987654321.pdf")
      private String buildingFileUrl;

      // homeId가 없는 경우 사용할 매물 정보
      @ApiModelProperty(value = "주소 (매물 ID가 없는 경우)", example = "서울특별시 강남구 테헤란로 123")
      private String address;

      @ApiModelProperty(value = "매물 가격 (매물 ID가 없는 경우)", example = "50000")
      private Long propertyPrice;

      @ApiModelProperty(value = "거래 유형 (매물 ID가 없는 경우)", example = "JEONSE")
      private String leaseType;

      @ApiModelProperty(value = "주거 유형 (매물 ID가 없는 경우)", example = "APARTMENT")
      private String residenceType;

      @ApiModelProperty(value = "소유자명 (매물 ID가 없는 경우)", example = "홍길동")
      private String registeredUserName;
}
