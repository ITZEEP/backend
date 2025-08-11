package org.scoula.domain.precontract.dto.common;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

/** 계약 중 본인 인증 정보 DTO */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "본인 인증 정보 DTO")
public class IdentityVerificationInfoDTO {

      @ApiModelProperty(value = "이름", required = true, example = "홍길동")
      private String name;

      @ApiModelProperty(value = "주민등록번호 앞자리 (6자리)", required = true, example = "901231")
      @NotBlank(message = "주민등록번호 앞자리는 필수입니다")
      @Pattern(regexp = "^\\d{6}$", message = "주민등록번호 앞자리는 6자리 숫자여야 합니다")
      private String ssnFront;

      @ApiModelProperty(value = "주민등록번호 뒷자리 (7자리)", required = true, example = "1234567")
      @NotBlank(message = "주민등록번호 뒷자리는 필수입니다")
      @Pattern(regexp = "^\\d{7}$", message = "주민등록번호 뒷자리는 7자리 숫자여야 합니다")
      private String ssnBack;

      @ApiModelProperty(
              value = "본인 인증 시점 (START: 계약 시작, END: 계약 종료)",
              required = true,
              example = "START")
      private ContractStep contractStep;

      @ApiModelProperty(value = "주소 1 (도로명)", required = true, example = "서울특별시 강남구 테헤란로 123")
      private String addr1;

      @ApiModelProperty(value = "주소 2 (상세주소)", required = true, example = "101동 202호")
      private String addr2;

      @ApiModelProperty(value = "발급일자 (YYYYMMDD)", required = true, example = "20230115")
      @NotBlank(message = "발급일자는 필수입니다")
      @Pattern(regexp = "^\\d{8}$", message = "발급일자는 YYYYMMDD 형식이어야 합니다")
      private String issuedDate;

      @ApiModelProperty(value = "휴대폰 번호", example = "01012345678")
      private String phoneNumber;

      public enum ContractStep {
          START,
          END
      }
}
