package org.scoula.domain.fraud.dto.common;

import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

/**
 * 파싱된 등기부등본 데이터 DTO
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@ApiModel(description = "파싱된 등기부등본 데이터")
public class ParsedRegistryDataDto {

      @ApiModelProperty(value = "지번 주소", example = "서울특별시 송파구 신천동 29 롯데월드타워앤드롯데월드몰 제월드타워동")
      private String regionAddress;

      @ApiModelProperty(value = "도로명 주소", example = "서울특별시 송파구 올림픽로 300")
      private String roadAddress;

      @ApiModelProperty(value = "소유자 이름", example = "한은숙")
      private String ownerName;

      @ApiModelProperty(value = "소유자 생년월일", example = "1956-02-17")
      private String ownerBirthDate;

      @ApiModelProperty(value = "채무자", example = "한은숙")
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
