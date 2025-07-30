package org.scoula.domain.precontract.dto.tenant;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.scoula.domain.home.enums.ResidenceType;
import org.scoula.global.common.constant.Constants;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(description = "임차인 계약전 사전 정보 요청 DTO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantPreContractDTO {

      // 매물
      @ApiModelProperty(
              value = "매물 지역 주소",
              example = "서울시 강남구 신사동") // 이걸 두개로 나눠야 하나? 나눠서 가져올 수 있는지 확인
      private String addr1; // 지역 주소

      @ApiModelProperty(value = "매물 상세 주소", example = "123-45")
      private String addr2; // 상세 주소

      // 매물 종료 : 오피스텔
      @ApiModelProperty(value = "매물 종류", example = "APARTMENT")
      private ResidenceType residenceType;

      //    private LeaseType leaseType; // 전월세 타입 -> 이것도 있는것 같다.
      @ApiModelProperty(value = "계약 보증금 or 전세금", example = "5") // -> 이거 int 인지 확인하기
      private String depositPrice; // 계약 보증금 or 전세금

      @ApiModelProperty(value = "월세비", example = "50")
      private String monthlyRent; // 월세비 (월세일때만)

      // 매물 사진
      @ApiModelProperty(value = "매물 대표 사진", example = "url") // url 맞나? 예시 넣어도 좋을듯
      private String imageUrl; // 매물 사진 주소

      // 사기 위험도 -> 다른데 있는건가??
      // 위험도
      @ApiModelProperty(value = "사기 위험도 조회 날짜", example = "2025-07-22") // 해당 날짜니까 이게 맞는지 확인, 시간도?
      private LocalDateTime riskCheckedAt; // 조회 날짜

      /////////////////////////////////

      @ApiModelProperty(value = "[PK/FK] 임차인 계약전 사전 정보 ID / 계약 채팅 ID", example = "1", required = true)
      private Long contractChatId;

      @ApiModelProperty(value = "[FK] 본인 인증 ID", example = "1", required = true)
      private Long identityId;

      @ApiModelProperty(value = "[FK] 사기 위험도 ID", example = "1", required = true)
      private Long riskckId;

      @ApiModelProperty(value = "사기 위험도 등급", example = "SAFE", allowableValues = "SAFE,WARN,DANGER")
      private String riskType;

      @ApiModelProperty(value = "전세 or 월세", example = "JEONSE", allowableValues = "JEONSE,WOLSE")
      private String rentType;

      @ApiModelProperty(value = "(전세 or 월세) 대출 계획", example = "true", allowableValues = "true,false")
      private Boolean loanPlan;

      @ApiModelProperty(
              value = "(전세 or 월세) 보증 보험 가입 계획",
              example = "true",
              allowableValues = "true,false")
      private Boolean insurancePlan;

      @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Constants.DateTime.DEFAULT_DATE_FORMAT)
      @ApiModelProperty(value = "입주 예정일", example = "2025-07-22")
      private LocalDate expectedMoveInDate;

      @ApiModelProperty(
              value = "계약 기간",
              example = "YEAR_2",
              allowableValues = "YEAR_1,YEAR_2,YEAR_OVER_2")
      private String contractDuration;

      @ApiModelProperty(
              value = "재계약 갱신 의사",
              example = "UNDECIDED",
              allowableValues = "YES,NO,UNDECIDED")
      private String renewalIntent;

      @ApiModelProperty(value = "주요설비 보수 필요 여부", example = "false", allowableValues = "true,false")
      private Boolean facilityRepairNeeded;

      @ApiModelProperty(
              value = "입주 전 도배, 장판, 청소 필요 여부",
              example = "true",
              allowableValues = "true,false")
      private Boolean interiorCleaningNeeded;

      @ApiModelProperty(
              value = "벽걸이, tv, 에어컨 설치 계획",
              example = "true",
              allowableValues = "true,false")
      private Boolean applianceInstallationPlan;

      @ApiModelProperty(value = "반려동물 여부", example = "false", allowableValues = "true,false")
      private Boolean hasPet;

      @ApiModelProperty(value = "반려동물 종", example = "강아지")
      private String petInfo;

      @ApiModelProperty(value = "반려동물 수", example = "1")
      private Long petCount;

      @ApiModelProperty(value = "실내 흡연 계획", example = "false", allowableValues = "true,false")
      private Boolean indoorSmokingPlan;

      @ApiModelProperty(value = "중도 퇴거 가능성", example = "false", allowableValues = "true,false")
      private Boolean earlyTerminationRisk;

      @ApiModelProperty(
              value = "거주 외 목적으로 사용 할 계획",
              example = "NONE",
              allowableValues = "BUSINESS,LODGING,NONE")
      private String nonresidentialUsePlan;

      @ApiModelProperty(value = "임대인에게 남길 요청 사항", example = "엘리베이터 점검일 피해서 입주 조율 가능할까요?")
      private String requestToOwner;

      @JsonFormat(
              shape = JsonFormat.Shape.STRING,
              pattern = Constants.DateTime.DEFAULT_DATETIME_FORMAT)
      @ApiModelProperty(value = "계약 전 사전 정보 작성 완료 시간", example = "2025-07-22 10:30:00")
      private LocalDateTime checkedAt;

      @ApiModelProperty(value = "거주 인원", example = "1")
      private Integer residentCount;

      @ApiModelProperty(value = "직업", example = "외교관")
      private String occupation;

      @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Constants.Number.PHONE)
      @ApiModelProperty(value = "비상 연락처", example = "010-1234-5678")
      private String emergencyContact;

      @ApiModelProperty(value = "관계", example = "남편")
      private String relation;

}
