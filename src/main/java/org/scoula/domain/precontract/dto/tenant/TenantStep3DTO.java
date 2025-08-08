package org.scoula.domain.precontract.dto.tenant;

import org.scoula.global.common.constant.Constants;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(description = "임차인 계약전 step3")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantStep3DTO {
      @ApiModelProperty(value = "실내 흡연 계획", example = "false", allowableValues = "true,false")
      private Boolean indoorSmokingPlan;

      @ApiModelProperty(value = "중도 퇴거 가능성", example = "false", allowableValues = "true,false")
      private Boolean earlyTerminationRisk;

      @ApiModelProperty(
              value = "거주 외 목적으로 사용 할 계획",
              example = "NONE",
              allowableValues = "BUSINESS,LODGING,NONE")
      private String nonresidentialUsePlan;

      @ApiModelProperty(value = "거주 인원", example = "5")
      private Integer residentCount;

      @ApiModelProperty(value = "임차인 직업", example = "개발자")
      private String occupation;

      @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Constants.Number.PHONE)
      @ApiModelProperty(value = "비상 연락처", example = "010-1234-5678")
      private String emergencyContact;

      @ApiModelProperty(value = "비상 연락처와의 관계", example = "남편")
      private String relation;

      @ApiModelProperty(value = "임대인에게 남길 요청 사항", example = "엘리베이터 점검일 피해서 입주 조율 가능할까요?")
      private String requestToOwner;
}
