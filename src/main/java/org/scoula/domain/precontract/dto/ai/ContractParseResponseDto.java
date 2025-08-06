package org.scoula.domain.precontract.dto.ai;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

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
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContractParseResponseDto {

      @JsonProperty("filename")
      private String filename;

      @JsonProperty("document_type")
      private String documentType;

      @JsonProperty("parsed_data")
      private ParsedData parsedData;

      @Getter
      @Setter
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      @JsonIgnoreProperties(ignoreUnknown = true)
      public static class ParsedData {

          @JsonProperty("file_name")
          private String fileName;

          @JsonProperty("extracted_at")
          private String extractedAt;

          @JsonProperty("source")
          private String source;

          @JsonProperty("special_terms")
          private List<String> specialTerms;

          @JsonProperty("raw_text")
          private String rawText;
      }
}
