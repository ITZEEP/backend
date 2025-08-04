package org.scoula.domain.fraud.dto.response;

import org.scoula.domain.fraud.dto.common.ParsedRegistryDataDto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

/**
 * 등기부등본 파싱 응답 DTO
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@ApiModel(description = "등기부등본 파싱 응답")
public class RegistryParseResponse {

      @ApiModelProperty(value = "파일명", example = "롯데타워 4409호 등기부등본.pdf")
      private String filename;

      @ApiModelProperty(value = "문서 타입", example = "register")
      @JsonProperty("document_type")
      private String documentType;

      @ApiModelProperty(value = "파싱된 데이터")
      @JsonProperty("parsed_data")
      private ParsedRegistryDataDto parsedData;
}
