package org.scoula.domain.contract.document;

import java.time.LocalDate;
import java.util.Collections;
import java.util.stream.Collectors;

import org.scoula.domain.contract.dto.ContractDTO;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "FINAL_CONTRACT")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractMongoDocument {

      @Id private String id;

      @Field("contractChatId")
      private Long contractChatId;

      // 제 1조 계약 당사자
      private String ownerName;
      private String ownerAddr;

      @JsonInclude(JsonInclude.Include.ALWAYS)
      private String ownerPhoneNum;

      private String buyerName;
      private String buyerAddr;

      @JsonInclude(JsonInclude.Include.ALWAYS)
      private String buyerPhoneNum;

      // 제 2조 임대물건의 표시
      private String homeAddr1;
      private String homeAddr2;
      private String residenceType;
      private float exclusiveArea;
      private int homeFloor;

      // 제 3조 임대차 기간 및 임료
      @Field("contractStartDate")
      private String contractStartDate;

      @Field("contractEndDate")
      private String contractEndDate; // 계산해서 넣기

      private int depositPrice;
      private int monthlyRent;
      private int maintenanceFee;

      // 제 4조 특약사항
      @JsonInclude(JsonInclude.Include.ALWAYS)
      private java.util.List<SpecialContract> specialContracts;

      @Data
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      public static class SpecialContract {
          private Integer order;
          private String title;
          private String content;
      }

      public static ContractMongoDocument toDocument(ContractDTO dto, LocalDate contractEndDate) {
          return ContractMongoDocument.builder()
                  .contractChatId(dto.getContractChatId())
                  .ownerName(dto.getOwnerName())
                  .ownerAddr(dto.getOwnerAddr())
                  .ownerPhoneNum(dto.getOwnerPhoneNum())
                  .buyerName(dto.getBuyerName())
                  .buyerAddr(dto.getBuyerAddr())
                  .buyerPhoneNum(dto.getBuyerPhoneNum())
                  .homeAddr1(dto.getHomeAddr1())
                  .homeAddr2(dto.getHomeAddr2())
                  .residenceType(dto.getResidenceType())
                  .exclusiveArea(dto.getExclusiveArea())
                  .homeFloor(dto.getHomeFloor())
                  .contractStartDate(dto.getContractStartDate().toString())
                  .contractEndDate(contractEndDate.toString())
                  .depositPrice(dto.getDepositPrice())
                  .monthlyRent(dto.getMonthlyRent())
                  .maintenanceFee(dto.getMaintenanceFee())
                  .specialContracts(
                          (dto.getSpecialContracts() == null
                                          ? Collections.<ContractDTO.SpecialContractDTO>emptyList()
                                          : dto.getSpecialContracts())
                                  .stream()
                                          .map(
                                                  s ->
                                                          SpecialContract.builder()
                                                                  .order(s.getOrder())
                                                                  .title(s.getTitle())
                                                                  .content(s.getContent())
                                                                  .build())
                                          .collect(Collectors.toList()))
                  .build();
      }
}
