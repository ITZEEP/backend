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

      @ApiModelProperty(value = "매물 ID", required = true, example = "10")
      @NotNull(message = "매물 ID는 필수입니다")
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
}
