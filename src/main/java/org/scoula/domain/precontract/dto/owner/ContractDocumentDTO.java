package org.scoula.domain.precontract.dto.owner;

import java.util.List;

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
@ApiModel(description = "특약 저장 요청 DTO")
public class ContractDocumentDTO {
      @ApiModelProperty(value = "임대인 사전 조사 ID", required = true)
      private Long ownerPrecheckId;

      @ApiModelProperty(value = "특약 목록", required = true)
      private List<String> specialTerms;

      @ApiModelProperty(value = "파일명")
      private String filename;

      @ApiModelProperty(value = "문서 타입")
      private String documentType;

      @ApiModelProperty(value = "추출 시간")
      private String extractedAt;

      @ApiModelProperty(value = "소스")
      private String source;

      @ApiModelProperty(value = "원본 텍스트")
      private String rawText;
}
