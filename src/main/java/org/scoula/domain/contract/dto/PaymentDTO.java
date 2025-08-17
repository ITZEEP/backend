package org.scoula.domain.contract.dto;

import org.scoula.domain.contract.document.ContractMongoDocument;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(description = "금액 조율에 필요한 금액들")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDTO {

      @ApiModelProperty(value = "보증금", example = "50000")
      private int depositPrice;

      @ApiModelProperty(value = "월세", example = "50000")
      private int monthlyRent;

      public static PaymentDTO toDTO(ContractMongoDocument document) {
          return PaymentDTO.builder()
                  .depositPrice(document.getDepositPrice())
                  .monthlyRent(document.getMonthlyRent())
                  .build();
      }
}
