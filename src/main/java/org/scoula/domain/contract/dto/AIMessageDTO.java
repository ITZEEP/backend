package org.scoula.domain.contract.dto;

import java.time.LocalDate;

import org.scoula.domain.contract.document.ContractMongoDocument;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(description = "계약채팅에 들어감 ")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIMessageDTO {
      // 임차인 이름
      private String ownerName;
      // 임대인 이름
      private String buyerName;

      // 계약 기간
      private LocalDate contractStartDate;
      private LocalDate contractEndDate;

      // 전월세
      private String rentType;
      // 보증금
      // 월세
      // 관리비
      private int depositPrice;
      private int monthlyRent;
      private int maintenanceFee;

      // 적합성 분석 개수

      public static AIMessageDTO toDTO(ContractMongoDocument document) {
          return AIMessageDTO.builder()
                  .ownerName(document.getOwnerName())
                  .buyerName(document.getBuyerName())
                  .contractStartDate(LocalDate.parse(document.getContractStartDate()))
                  .contractEndDate(LocalDate.parse(document.getContractEndDate()))
                  .depositPrice(document.getDepositPrice())
                  .monthlyRent(document.getMonthlyRent())
                  .maintenanceFee(document.getMaintenanceFee())
                  .build();
      }
}
