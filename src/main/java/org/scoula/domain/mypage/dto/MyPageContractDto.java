package org.scoula.domain.mypage.dto;

import java.time.LocalDateTime;

import org.scoula.domain.home.enums.LeaseType;
import org.scoula.domain.home.enums.ResidenceType;
import org.scoula.domain.mypage.enums.ContractStatus;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "마이페이지 계약서 정보")
public class MyPageContractDto {
      @ApiModelProperty(value = "계약서 ID", example = "1")
      private Long contractId;

      @ApiModelProperty(value = "매물 주소", example = "서울시 강남구 역삼동 123-45")
      private String address;

      @ApiModelProperty(value = "건물 유형", example = "APARTMENT")
      private ResidenceType buildingType;

      @ApiModelProperty(value = "계약 일시", example = "2024-01-15T10:30:00")
      private LocalDateTime contractDate;

      @ApiModelProperty(value = "계약서 파일 URL", example = "https://example.com/contracts/sample.pdf")
      private String fileUrl;

      @ApiModelProperty(
              value = "계약 상태",
              example = "STEP1",
              allowableValues =
                      "STEP0, STEP1, STEP2, STEP4, ROUND0, ROUND1, ROUND2, ROUND3, COMPLETED")
      private String status; // MyBatis에서 String으로 받음

      // Enum 변환 메서드
      public ContractStatus getStatusEnum() {
          return status != null ? ContractStatus.fromValue(status) : null;
      }

      public void setStatusEnum(ContractStatus statusEnum) {
          this.status = statusEnum != null ? statusEnum.getValue() : null;
      }

      @ApiModelProperty(value = "임대 유형", example = "JEONSE")
      private LeaseType leaseType;
}
