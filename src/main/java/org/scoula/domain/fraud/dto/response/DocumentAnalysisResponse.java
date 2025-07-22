package org.scoula.domain.fraud.dto.response;

import java.time.LocalDateTime;

import org.scoula.domain.fraud.dto.common.BuildingDocumentDto;
import org.scoula.domain.fraud.dto.common.RegistryDocumentDto;

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
@ApiModel(description = "PDF 문서 분석 응답")
public class DocumentAnalysisResponse {

      @ApiModelProperty(value = "매물 ID", example = "10")
      private Long homeId;

      @ApiModelProperty(value = "등기부등본 정보")
      private RegistryDocumentDto registryDocument;

      @ApiModelProperty(value = "건축물대장 정보")
      private BuildingDocumentDto buildingDocument;

      @ApiModelProperty(value = "등기부등본 분석 상태", example = "SUCCESS")
      private String registryAnalysisStatus;

      @ApiModelProperty(value = "건축물대장 분석 상태", example = "SUCCESS")
      private String buildingAnalysisStatus;

      @ApiModelProperty(value = "등기부등본 파일 분석일시", example = "2024-01-15T10:30:00")
      @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
      private LocalDateTime registryAnalyzedAt;

      @ApiModelProperty(value = "건축물대장 파일 분석일시", example = "2024-01-15T10:30:00")
      @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
      private LocalDateTime buildingAnalyzedAt;

      @ApiModelProperty(value = "전체 분석 소요시간(초)", example = "3.5")
      private Double processingTime;

      @ApiModelProperty(value = "오류 메시지")
      private String errorMessage;

      @ApiModelProperty(value = "경고 메시지 목록")
      private String[] warnings;
}
