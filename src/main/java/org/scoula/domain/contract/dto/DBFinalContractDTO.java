package org.scoula.domain.contract.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.scoula.domain.precontract.enums.ContractDuration;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(description = "DB에 있는 최종 계약서에 들어가는 내용들")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DBFinalContractDTO {

      private String leaseType;
      private String landCategory; // home detail _ 토지 지목
      private BigDecimal area; // home detail _ 토지 면적

      private String buildingStructure; // 건물 구조 _ '철근콘크리트 구조'로 고정하기
      //      private String purpose; // 성엽님 _ 건물 용도
      //      private float totalFloorArea; // 성엽님 _ 건물 면적

      private boolean hasTaxArrears; // owner pre contract check _ 미납 국세, 지방세 여부
      private boolean hasPriorFixedDate; // owner pre contract check _ 선순위 확정일자 현황

      private int paymentDueDay; // owner_wolse_info _ 매월 지불 일자
      private String bankAccount; // owner pre contract check _ 입금 계좌 & 은행 : owner_bank_name &
      // owner_account_number

      private LocalDate expectedMoveInDate; // tenant pre contract check 입주 날짜 -> === 특약사항에도 들어감 ===

      private ContractDuration
              contractDuration; // Tenant pre contract check에서 contract_duration으로 퇴거 날짜 계산해서 넣기
      private LocalDate contractDate; // 계약하는 날짜 now()써서 하기

      private String ownerSsnFront; // identity verification ssnFront + ssnBack 합쳐서 넣기
      private String ownerSsnBack;
      private String buyerSsnFront;
      private String buyerSsnBack;

      // 주소 정보
      private String homeAddr1;
      private String homeAddr2;
}
