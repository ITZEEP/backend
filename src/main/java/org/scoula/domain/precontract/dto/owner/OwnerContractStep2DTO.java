package org.scoula.domain.precontract.dto.owner;

import java.util.List;

import org.scoula.domain.precontract.vo.RestoreCategoryVO;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@ApiModel(description = "계약 조건 설정 요청/응답 DTO (서브 2)")
public class OwnerContractStep2DTO {
      @ApiModelProperty(value = "임대인 계약 사전 조사 id (owner_precontract_check PK)", required = true)
      private Long ownerPrecheckId;

      @ApiModelProperty(
              value = "원상복구 범위 카테고리 ID 목록 또는 이름 목록",
              example = "'['1, 2, 3']' or '['\"벽지\", \"가구\"']'")
      private List<RestoreCategoryVO> restoreCategories;

      @ApiModelProperty(value = "입주 시 상태 기록 여부", required = true)
      private Boolean hasConditionLog;

      @ApiModelProperty(value = "중도 퇴거 위약금 여부", required = true)
      private Boolean hasPenalty;

      @ApiModelProperty(value = "계약 연장 우선 협의 조건 여부", required = true)
      private Boolean hasPriorityForExtension;

      @ApiModelProperty(value = "계약 연장 금액 자동 조정 여부", required = true)
      private Boolean hasAutoPriceAdjustment;

      @ApiModelProperty(value = "전세권 설정 허용 여부 (전세일 경우에만)", required = false)
      private Boolean allowJeonseRightRegistration;
}
