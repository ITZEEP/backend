package org.scoula.domain.fraud.dto.ai;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiParseResponse {
      private boolean success;
      private String message;
      private AiParseData data;
      private String error;

      @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX")
      private String timestamp;

      @Getter
      @Setter
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      public static class AiParseData {
          private String filename;

          @JsonProperty("document_type")
          private String documentType;

          @JsonProperty("parsed_data")
          private Object parsedData; // Object로 받아서 나중에 적절한 타입으로 변환
      }
}
