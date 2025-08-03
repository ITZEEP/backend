package org.scoula.domain.fraud.dto.common;

import java.time.LocalDate;
import java.util.List;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@ApiModel(description = "등기부등본 정보")
public class RegistryDocumentDto {

      @ApiModelProperty(value = "지역관련주소 (지번)", required = true, example = "서울특별시 강남구 역삼동 123-45번지")
      @NotBlank(message = "지역관련주소는 필수입니다")
      private String regionAddress;

      @ApiModelProperty(value = "도로명 주소", required = true, example = "서울특별시 강남구 테헤란로 123")
      @NotBlank(message = "도로명 주소는 필수입니다")
      private String roadAddress;

      @ApiModelProperty(value = "소유자 이름", required = true, example = "홍길동")
      @NotBlank(message = "소유자 이름은 필수입니다")
      private String ownerName;

      @ApiModelProperty(value = "소유자 생년월일", example = "1980-01-15")
      @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
      private LocalDate ownerBirthDate;

      @ApiModelProperty(value = "채무자", example = "홍길동")
      private String debtor;

      @ApiModelProperty(value = "근저당권자 리스트")
      private List<MortgageeDto> mortgageeList;

      @ApiModelProperty(value = "가압류 여부", example = "false")
      private Boolean hasSeizure;

      @ApiModelProperty(value = "경매 여부", example = "false")
      private Boolean hasAuction;

      @ApiModelProperty(value = "소송 여부", example = "false")
      private Boolean hasLitigation;

      @ApiModelProperty(value = "압류 여부", example = "false")
      private Boolean hasAttachment;
}
