package org.scoula.domain.fraud.dto.common;

import java.time.LocalDate;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

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
@ApiModel(description = "건축물대장 정보")
public class BuildingDocumentDto {

      @ApiModelProperty(value = "대지위치 (지번)", required = true, example = "서울특별시 강남구 역삼동 123-45번지")
      @NotBlank(message = "대지위치는 필수입니다")
      private String siteLocation;

      @ApiModelProperty(value = "도로명주소", required = true, example = "서울특별시 강남구 테헤란로 123")
      @NotBlank(message = "도로명주소는 필수입니다")
      private String roadAddress;

      @ApiModelProperty(value = "연면적 (㎡)", required = true, example = "84.5")
      @NotNull(message = "연면적은 필수입니다")
      private Double totalFloorArea;

      @ApiModelProperty(value = "용도", required = true, example = "아파트")
      @NotBlank(message = "용도는 필수입니다")
      private String purpose;

      @ApiModelProperty(value = "층수", required = true, example = "15")
      @NotNull(message = "층수는 필수입니다")
      private Integer floorNumber;

      @ApiModelProperty(value = "사용승인일", required = true, example = "2020-03-20")
      @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
      @NotNull(message = "사용승인일은 필수입니다")
      private LocalDate approvalDate;

      @ApiModelProperty(value = "위반건축물 여부", required = true, example = "false")
      @NotNull(message = "위반건축물 여부는 필수입니다")
      private Boolean isViolationBuilding;
}
