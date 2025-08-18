package org.scoula.domain.contract.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.scoula.domain.contract.document.ContractMongoDocument;
import org.scoula.domain.precontract.vo.IdentityVerificationInfoVO;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(description = "AI에 보낼 최종 계약서에 들어가는 내용들")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveFinalContractDTO {

      private Boolean leaseType; // 전세: true, 월세: false
      private String ownerNickname; // user _ 임대인 이름
      private String buyerNickname; // user_임차인 이름

      // 임차주택의 표시
      private String addr1; // home _ addr1
      private String landCategory; // home detail _ 토지 지목
      private String area; // home detail _ 토지 면적 (String으로 변경)

      private String buildingStructure; // 건물 구조 _ '철근콘크리트 구조'로 고정하기
      private String purpose; // 성엽님 _ 건물 용도
      private String totalFloorArea; // 성엽님 _ 건물 면적 (String으로 변경)

      private String addr2; // home_ addr2 _ 임차할 부분 주소
      private String supplyArea; // home _ 임차할 부분 면적 (String으로 변경)

      private Boolean hasTaxArrears; // owner pre contract check _ 미납 국세, 지방세 여부
      private Boolean hasPriorFixedDate; // owner pre contract check _ 선순위 확정일자 현황

      // 계약 내용
      private String textDepositPrice; // 보증금 금액 : 한글
      private String depositPrice; // home_보증금 금액 : 숫자만 (String으로 변경)
      private String monthlyRent; // home _ 차임(월세)원정 (String으로 변경)
      private String paymentDueDay; // owner_wolse_info _ 매월 지불 일자 (String으로 변경)
      private String bankAccount; // owner wolse info _ 입금 계좌 & 은행
      private String textMaintenanceFee; // home _ 관리비 : 한글
      private String maintenanceFee; // home : 숫자만 (String으로 변경)

      // 2조 임대차기간
      private String expectedMoveInYear; // tenant pre contract check 입주 날짜
      private String expectedMoveInMonth;
      private String expectedMoveInDay;

      private String expectedMoveOutYear; // 퇴거 날짜
      private String expectedMoveOutMonth;
      private String expectedMoveOutDay;

      private String contractDateYear; // 계약하는 날짜
      private String contractDateMonth;
      private String contractDateDay;

      // 마지막 사인
      private String ownerAddr; // identity_verifiacation에서 addr1 + addr2 합쳐서 넣기
      private String ownerSsn; // identity verification ssnFront + ssnBack 합쳐서 넣기
      private String ownerPhoneNumber; // identity verification

      private String buyerAddr;
      private String buyerSsn;
      private String buyerPhoneNumber;

      // 특약사항
      private List<String> special; // 특약사항 리스트

      // 서명 이미지 (base64 인코딩)
      private String ownerSign1Base64;
      private String ownerSign2Base64;
      private String ownerSign3Base64;
      private String buyerSignBase64;

      public static SaveFinalContractDTO toDTO(
              DBFinalContractDTO dto,
              boolean leaseType,
              String buildingStructure,
              String textDepositPrice,
              String textMaintenanceFee,
              LocalDate expectedMoveOut,
              String ownerSsn,
              String buyerSsn,
              ContractMongoDocument document,
              IdentityVerificationInfoVO ownerVO,
              IdentityVerificationInfoVO buyerVO) {
          return SaveFinalContractDTO.builder()
                  .leaseType(leaseType) // boolean 값 그대로 전달
                  .ownerNickname(document.getOwnerName())
                  .buyerNickname(document.getBuyerName())
                  .addr1(document.getHomeAddr1())
                  .landCategory(dto.getLandCategory())
                  .area(String.valueOf(dto.getArea()))
                  .buildingStructure(buildingStructure)
                  .purpose("주택") // 기본값 설정
                  .totalFloorArea("100") // 기본값 설정
                  .addr2(document.getHomeAddr2())
                  .supplyArea(String.valueOf(document.getExclusiveArea()))
                  .hasTaxArrears(dto.isHasTaxArrears())
                  .hasPriorFixedDate(dto.isHasPriorFixedDate())
                  .textDepositPrice(textDepositPrice)
                  .depositPrice(String.valueOf(document.getDepositPrice()))
                  .monthlyRent(String.valueOf(document.getMonthlyRent()))
                  .paymentDueDay(String.valueOf(dto.getPaymentDueDay()))
                  .bankAccount(dto.getBankAccount())
                  .textMaintenanceFee(textMaintenanceFee)
                  .maintenanceFee(String.valueOf(document.getMaintenanceFee()))
                  .expectedMoveInYear(String.valueOf(dto.getExpectedMoveInDate().getYear()))
                  .expectedMoveInMonth(String.valueOf(dto.getExpectedMoveInDate().getMonthValue()))
                  .expectedMoveInDay(String.valueOf(dto.getExpectedMoveInDate().getDayOfMonth()))
                  .expectedMoveOutYear(String.valueOf(expectedMoveOut.getYear()))
                  .expectedMoveOutMonth(String.valueOf(expectedMoveOut.getMonthValue()))
                  .expectedMoveOutDay(String.valueOf(expectedMoveOut.getDayOfMonth()))
                  .contractDateYear(String.valueOf(dto.getContractDate().getYear()))
                  .contractDateMonth(String.valueOf(dto.getContractDate().getMonthValue()))
                  .contractDateDay(String.valueOf(dto.getContractDate().getDayOfMonth()))
                  .ownerAddr(ownerVO.getAddr1() + " " + ownerVO.getAddr2())
                  .ownerSsn(ownerSsn)
                  .ownerPhoneNumber(ownerVO.getPhoneNumber())
                  .buyerAddr(buyerVO.getAddr1() + " " + buyerVO.getAddr2())
                  .buyerSsn(buyerSsn)
                  .buyerPhoneNumber(buyerVO.getPhoneNumber())
                  .special(
                          document.getSpecialContracts() != null
                                  ? document.getSpecialContracts().stream()
                                          .map(sc -> sc.getContent())
                                          .collect(Collectors.toList())
                                  : null)
                  .build();
      }
}
