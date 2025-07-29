package org.scoula.domain.precontract.dto;

import java.time.LocalDate;

import org.scoula.global.common.constant.Constants;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(description = "임차인 계약전 step1")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantStep1DTO {

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
}
