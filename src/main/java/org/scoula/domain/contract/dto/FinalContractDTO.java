package org.scoula.domain.contract.dto;

import java.io.File;
import java.math.BigDecimal;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(description = "최종 계약서")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class FinalContractDTO {

      private Boolean leaseType; // home _ 전세 : True / 월세 : False
      private String ownerNickname; // user _ 임대인 이름
      private String buyerNickname; // user_임차인 이름

      // 임차주택의 표시
      private String addr1; // home _ addr1
      private String landCategory; // home detail _ 토지 지목
      private BigDecimal area; // home detail _ 토지 면적

      private String buildingStructure; // 건물 구조 _ '철근콘크리트 구조'로 고정하기
      private String purpose; // 성엽님 _ 건물 용도
      private float totalFloorArea; // 성엽님 _ 건물 면적

      private String addr2; // home_ addr2 _ 임차할 부분 주소
      private float supplyArea; // home _ 임차할 부분 면적

      private boolean hasTaxArrears; // owner pre contract check _ 미납 국세, 지방세 여부
      private boolean hasPriorFixedDate; // owner pre contract check _ 선순위 확정일자 현황

      // 계약 내용
      private String textDepositPrice; // 보증금 금액 : 한글
      private int depositPrice; // home_보증금 금액 : 숫자만 (,도 없음)
      private int monthlyRent; // home _ 차임(월세)원정
      private int paymentDueDay; // owner_wolse_info _ 매월 지불 일자
      private String
              bankAccount; // owner wolse info _ 입금 계좌 & 은행 : owner_bank_name & owner_account_number
      private String textMaintenanceFee; // home _ 관리비 : 한글
      private int maintenanceFee; // home : 숫자만 (,도 없음)

      // 2조 임대차기간
      private int expectedMoveInYear; // tenant pre contract check 입주 날짜 -> === 특약사항에도 들어감 ===
      private int expectedMoveInMonth;
      private int expectedMoveInDay; // -> 이거 그냥 하나로 넘겨서 나누면 될듯!

      private int
              expectedMoveOutYear; // Tenant pre contract check에서 contract_duration으로 퇴거 날짜 계산해서 넣기
      private int expectedMoveOutMonth;
      private int expectedMoveOutDay;

      private int contractDateYear; // 계약하는 날짜 now()써서 하기
      private int contractDateMonth;
      private int contractDateDay;

      // 마지막 사인
      private String ownerAddr; // identity_verifiacation에서 addr1 + addr2 합쳐서 넣기
      private String ownerSsn; // identity verification ssnFront + ssnBack 합쳐서 넣기
      private String ownerPhoneNumber; // identity verification
      // 임대인 이름은 위쪽에 있음

      private String buyerAddr;
      private String buyerSsn;
      private String buyerPhoneNumber;

      // ----------------------------

      private File ownerTaxSignature; // 미납 국세 지방세 Nullable
      private File ownerPrioritySignature; // 선순위 확정일자 현황 nullable

      private File ownerContractSignature;
      private File buyerContractSignature;

      private Boolean ownerMediationAgree; // 조정 동의 여부
      private Boolean buyerMediationAgree; // 조정 동의 여부

      //      private String contractKey; // 계약서 비밀번호

      //      public static FinalContractDTO toDTO(
      //              DBFinalContractDTO dto,
      //              boolean leaseType,
      //              String buildingStructure,
      //              String textDepositPrice,
      //              String textMaintenanceFee,
      //              LocalDate expectedMoveOut,
      //              String ownerSsn,
      //              String buyerSsn,
      //              ContractMongoDocument document,
      //              IdentityVerificationInfoVO ownerVO,
      //              IdentityVerificationInfoVO buyerVO,
      //              Boolean mediationAgree,
      //              Boolean mediationAgreed) {
      //          return FinalContractDTO.builder()
      //                  .leaseType(leaseType)
      //                  .ownerNickname(document.getOwnerName())
      //                  .buyerNickname(document.getBuyerName())
      //                  .addr1(document.getHomeAddr1())
      //                  .landCategory(dto.getLandCategory())
      //                  .area(dto.getArea())
      //                  .buildingStructure(buildingStructure)
      //                  //                  .purpose(dto.getPurpose())
      //                  //                  .totalFloorArea(dto.getTotalFloorArea())
      //                  .addr2(document.getHomeAddr2())
      //                  .supplyArea(document.getExclusiveArea())
      //                  .hasTaxArrears(dto.isHasTaxArrears())
      //                  .hasPriorFixedDate(dto.isHasPriorFixedDate())
      //                  .textDepositPrice(textDepositPrice)
      //                  .depositPrice(document.getDepositPrice())
      //                  .monthlyRent(document.getMonthlyRent())
      //                  .paymentDueDay(dto.getPaymentDueDay())
      //                  .bankAccount(dto.getBankAccount())
      //                  .textMaintenanceFee(textMaintenanceFee)
      //                  .maintenanceFee(document.getMaintenanceFee())
      //                  .expectedMoveInYear(dto.getExpectedMoveInDate().getYear())
      //                  .expectedMoveInMonth(dto.getExpectedMoveInDate().getMonthValue())
      //                  .expectedMoveInDay(dto.getExpectedMoveInDate().getDayOfMonth())
      //                  .expectedMoveOutYear(expectedMoveOut.getYear())
      //                  .expectedMoveOutMonth(expectedMoveOut.getMonthValue())
      //                  .expectedMoveOutDay(expectedMoveOut.getDayOfMonth())
      //                  .contractDateYear(dto.getContractDate().getYear())
      //                  .contractDateMonth(dto.getContractDate().getMonthValue())
      //                  .contractDateDay(dto.getContractDate().getDayOfMonth())
      //                  .ownerAddr(ownerVO.getAddr1() + " " + ownerVO.getAddr2())
      //                  .ownerSsn(ownerSsn)
      //                  .ownerPhoneNumber(ownerVO.getPhoneNumber())
      //                  .buyerAddr(buyerVO.getAddr1() + " " + buyerVO.getAddr2())
      //                  .buyerSsn(buyerSsn)
      //                  .buyerPhoneNumber(buyerVO.getPhoneNumber())
      //                  //                  .mediationAgree(mediationAgree)
      //                  .build();
      //      }
      //
      //      public static FinalContractDTO toDTOs(File file, SignedType signedType) {
      //          FinalContractDTO.FinalContractDTOBuilder builder = FinalContractDTO.builder();
      //
      //          switch (signedType) {
      //              case TAX:
      //                  builder.ownerTaxSignature(file);
      //                  break;
      //              case PRIORITY:
      //                  builder.ownerPrioritySignature(file);
      //                  break;
      //              case OWNER_CONTRACT:
      //                  builder.ownerContractSignature(file);
      //                  break;
      //              case BUYER_CONTRACT:
      //                  builder.buyerContractSignature(file);
      //                  break;
      //              default:
      //                  throw new IllegalArgumentException("지원하지 않는 서명 타입입니다: " + signedType);
      //          }
      //
      //          return builder.build();
      //      }
      //
      //      public static FinalContractDTO toAgreeDTO(Boolean mediationAgree, File contractPDF) {
      //          return FinalContractDTO.builder()
      //                  //                  .mediationAgree(mediationAgree)
      //                  //                  .contractPDF(contractPDF)
      //                  .build();
      //      }
}
