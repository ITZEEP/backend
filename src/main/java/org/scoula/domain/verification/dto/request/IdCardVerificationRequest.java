package org.scoula.domain.verification.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 주민등록증 진위 확인 요청 DTO
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "주민등록증 진위 확인 요청")
public class IdCardVerificationRequest {

      @ApiModelProperty(value = "이름", required = true, example = "홍길동")
      @NotBlank(message = "이름은 필수입니다")
      private String name;

      @ApiModelProperty(value = "주민등록번호 앞자리 (6자리)", required = true, example = "901231")
      @NotBlank(message = "주민등록번호 앞자리는 필수입니다")
      @Pattern(regexp = "^\\d{6}$", message = "주민등록번호 앞자리는 6자리 숫자여야 합니다")
      private String rrn1;

      @ApiModelProperty(value = "주민등록번호 뒷자리 (7자리)", required = true, example = "1234567")
      @NotBlank(message = "주민등록번호 뒷자리는 필수입니다")
      @Pattern(regexp = "^\\d{7}$", message = "주민등록번호 뒷자리는 7자리 숫자여야 합니다")
      private String rrn2;

      @ApiModelProperty(value = "발급일자 (YYYYMMDD)", required = true, example = "20230115")
      @NotBlank(message = "발급일자는 필수입니다")
      @Pattern(regexp = "^\\d{8}$", message = "발급일자는 YYYYMMDD 형식이어야 합니다")
      private String date;

      /** 로그용 마스킹된 정보 반환 민감정보 보호를 위해 주민번호 뒷자리를 마스킹합니다. */
      @JsonIgnore
      @ToString.Include
      public String getMaskedInfo() {
          return String.format(
                  "IdCardVerificationRequest(name=%s, rrn1=%s, rrn2=*****, date=%s)",
                  name != null ? name : "null",
                  rrn1 != null ? rrn1 : "null",
                  date != null ? date : "null");
      }

      /** toString 메서드 오버라이드 민감정보 보호를 위해 마스킹된 정보를 반환합니다. */
      @Override
      public String toString() {
          return getMaskedInfo();
      }
}
