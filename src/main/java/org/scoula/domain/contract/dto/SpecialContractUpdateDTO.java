package org.scoula.domain.contract.dto;

import java.util.List;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(description = "적법성 검사 후 수정할 특약사항")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecialContractUpdateDTO {
      private List<SpecialClauseDTO> specialClauses;

      @Data
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      public static class SpecialClauseDTO {
          private Integer order;
          private String title;
          private String content;
      }
}
