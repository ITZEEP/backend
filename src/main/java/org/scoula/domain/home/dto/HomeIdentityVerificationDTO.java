package org.scoula.domain.home.dto;

import static org.scoula.domain.home.constant.HomeIdentityConstants.*;

import java.time.LocalDate;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "매물 본인 인증 정보 DTO")
public class HomeIdentityVerificationDTO {

      @ApiModelProperty(value = "이름", required = true, example = "홍길동")
      @NotBlank(message = "이름은 필수입니다")
      private String name;

      @ApiModelProperty(value = "주민등록번호 앞자리 (6자리)", required = true, example = "900101")
      @NotBlank(message = "주민등록번호 앞자리는 필수입니다")
      @Pattern(regexp = SSN_FRONT_PATTERN, message = "주민등록번호 앞자리는 6자리 숫자여야 합니다")
      private String ssnFront;

      @ApiModelProperty(value = "주민등록번호 뒷자리 (7자리)", required = true, example = "1234567")
      @NotBlank(message = "주민등록번호 뒷자리는 필수입니다")
      @Pattern(regexp = SSN_BACK_PATTERN, message = "주믌등록번호 뒷자리는 7자리 숫자여야 합니다")
      private String ssnBack;

      @ApiModelProperty(value = "발급일자 (YYYYMMDD)", required = true, example = "20230115")
      @NotBlank(message = "발급일자는 필수입니다")
      @Pattern(regexp = ISSUED_DATE_PATTERN, message = "발급일자는 YYYYMMDD 형식이어야 합니다")
      private String issuedDate;

      @ApiModelProperty(value = "생년월일", required = true, example = "1990-01-01")
      @NotNull(message = "생년월일은 필수입니다")
      @JsonFormat(pattern = DATE_FORMAT_PATTERN)
      private LocalDate birthDate;
}
