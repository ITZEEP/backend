package org.scoula.domain.fraud.dto.common;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

/**
 * 근저당권자 정보 DTO
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
@ApiModel(description = "근저당권자 정보")
public class MortgageeDto {

      @ApiModelProperty(value = "순위 번호", example = "1")
      private Integer priorityNumber;

      @ApiModelProperty(value = "채권 최고액 (원)", example = "4680000000")
      private Long maxClaimAmount;

      @ApiModelProperty(value = "채무자", example = "한은숙")
      private String debtor;

      @ApiModelProperty(value = "근저당권자", example = "주식회사하나은행")
      private String mortgagee;
}
