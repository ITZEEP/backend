package org.scoula.domain.fraud.dto;

import java.time.LocalDateTime;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "사기 위험도 체크 정보")
public class FraudRiskCheckDto {
      @ApiModelProperty(value = "위험도 체크 ID")
      private Long riskckId;

      @ApiModelProperty(value = "사용자 ID")
      private Long userId;

      @ApiModelProperty(value = "집 ID")
      private Long homeId;

      @ApiModelProperty(value = "위험도 타입", example = "SAFE")
      private String riskType;

      @ApiModelProperty(value = "체크 일시")
      @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
      private LocalDateTime checkedAt;

      // 파일 정보
      @ApiModelProperty(value = "등기부등본 파일 URL")
      private String registryFileUrl;

      @ApiModelProperty(value = "건축물대장 파일 URL")
      private String buildingFileUrl;

      @ApiModelProperty(value = "등기부등본 파일 날짜")
      @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
      private LocalDateTime registryFileDate;

      @ApiModelProperty(value = "건축물대장 파일 날짜")
      @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
      private LocalDateTime buildingFileDate;

      // AI 파싱 데이터
      @ApiModelProperty(value = "지번 주소")
      private String regionAddress;

      @ApiModelProperty(value = "도로명 주소")
      private String roadAddress;

      @ApiModelProperty(value = "소유자명")
      private String ownerName;

      @ApiModelProperty(value = "소유자 생년월일")
      @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
      private Date ownerBirthDate;

      @ApiModelProperty(value = "채무자")
      private String debtor;

      @ApiModelProperty(value = "근저당권 정보 (JSON)")
      private String mortgageeInfo;

      @ApiModelProperty(value = "압류 여부")
      private Boolean hasSeizure;

      @ApiModelProperty(value = "경매 여부")
      private Boolean hasAuction;

      @ApiModelProperty(value = "소송 여부")
      private Boolean hasLitigation;

      @ApiModelProperty(value = "가압류 여부")
      private Boolean hasAttachment;

      @ApiModelProperty(value = "AI 파싱 완료 시간")
      @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
      private LocalDateTime parsedAt;

      // 건축물대장 파싱 데이터
      @ApiModelProperty(value = "대지위치")
      private String siteLocation;

      @ApiModelProperty(value = "연면적(㎡)")
      private Double totalFloorArea;

      @ApiModelProperty(value = "용도")
      private String purpose;

      @ApiModelProperty(value = "층수")
      private Integer floorNumber;

      @ApiModelProperty(value = "사용승인일")
      @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
      private Date approvalDate;

      @ApiModelProperty(value = "위반건축물 여부")
      private Boolean isViolationBuilding;
}
