package org.scoula.domain.fraud.dto.request;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
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

      @ApiModelProperty(value = "매물 ID", required = true, example = "10")
      @NotNull(message = "매물 ID는 필수입니다")
      private Long homeId;

      @ApiModelProperty(value = "주소", required = true, example = "서울특별시 강남구 테헤란로 123")
      @NotBlank(message = "주소는 필수입니다")
      private String address;

      @ApiModelProperty(value = "매물 가격 (전세인 경우 전세금, 월세인 경우 보증금)", required = true, example = "50000")
      @NotNull(message = "매물 가격은 필수입니다")
      private Long propertyPrice;

      @ApiModelProperty(value = "거래 유형", required = true, example = "JEONSE")
      @NotBlank(message = "거래 유형은 필수입니다")
      private String leaseType;

      @ApiModelProperty(value = "주거 유형", required = true, example = "APARTMENT")
      @NotBlank(message = "주거 유형은 필수입니다")
      private String residenceType;

      @ApiModelProperty(value = "소유자명", required = true, example = "홍길동")
      @NotBlank(message = "소유자명은 필수입니다")
      private String registeredUserName;

      @ApiModelProperty(value = "월세 (거래 유형이 WOLSE인 경우)", example = "100")
      private Long monthlyRent;

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
}
