package org.scoula.domain.contract.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.scoula.domain.contract.document.ContractMongoDocument;
import org.scoula.global.common.constant.Constants;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(description = "계약서에 들어가는 내용")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractDTO {

      private Long contractChatId;

      // 제 1조 계약 당사자
      private String ownerName;
      private String ownerAddr;
      private String ownerPhoneNum;

      private String buyerName;
      private String buyerAddr;
      private String buyerPhoneNum;

      // 제 2조 임대물건의 표시
      private String homeAddr1;
      private String homeAddr2;
      private String residenceType;
      private float exclusiveArea;
      private int homeFloor;

      // 제 3조 임대차 기간 및 임료
      @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Constants.DateTime.DEFAULT_DATE_FORMAT)
      private LocalDate contractStartDate;

      @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Constants.DateTime.DEFAULT_DATE_FORMAT)
      private LocalDate contractEndDate; // 계산해서 넣기

      private int depositPrice;
      private int monthlyRent;
      private int maintenanceFee;

      private List<SpecialContractDTO> specialContracts;

      @Data
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      public static class SpecialContractDTO {
          private Integer order;
          private String title;
          private String content;
      }

      public static ContractDTO toDTO(ContractMongoDocument document) {
          return ContractDTO.builder()
                  .contractChatId(document.getContractChatId())
                  .ownerName(document.getOwnerName())
                  .ownerAddr(document.getOwnerAddr())
                  .ownerPhoneNum(document.getOwnerPhoneNum())
                  .buyerName(document.getBuyerName())
                  .buyerAddr(document.getBuyerAddr())
                  .buyerPhoneNum(document.getBuyerPhoneNum())
                  .homeAddr1(document.getHomeAddr1())
                  .homeAddr2(document.getHomeAddr2())
                  .residenceType(document.getResidenceType())
                  .exclusiveArea(document.getExclusiveArea())
                  .homeFloor(document.getHomeFloor())
                  .contractStartDate(LocalDate.parse(document.getContractStartDate()))
                  .contractEndDate(LocalDate.parse(document.getContractEndDate()))
                  .depositPrice(document.getDepositPrice())
                  .monthlyRent(document.getMonthlyRent())
                  .maintenanceFee(document.getMaintenanceFee())
                  .specialContracts(
                          document.getSpecialContracts().stream()
                                  .map(
                                          documentSpecialContract ->
                                                  SpecialContractDTO.builder()
                                                          .content(
                                                                  documentSpecialContract
                                                                          .getContent())
                                                          .title(documentSpecialContract.getTitle())
                                                          .order(documentSpecialContract.getOrder())
                                                          .build())
                                  .toList())
                  .build();
      }

      public static ContractDTO toSpecialContractDTO(ContractMongoDocument document) {
          return ContractDTO.builder()
                  .specialContracts(
                          document.getSpecialContracts().stream()
                                  .map(
                                          c ->
                                                  SpecialContractDTO.builder()
                                                          .order(c.getOrder())
                                                          .title(c.getTitle())
                                                          .content(c.getContent())
                                                          .build())
                                  .collect(Collectors.toList()))
                  .build();
      }
}
