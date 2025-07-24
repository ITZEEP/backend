package org.scoula.domain.precontract.controller;

import org.scoula.domain.precontract.dto.TenantPreContractDTO;
import org.scoula.global.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(tags = "계약전 사전조사 API", description = "임대인/임차인 사전조사")
public interface PreContractController {

      @ApiOperation(value = "임차인 : 계약전 정보 저장", notes = "임차인의 계약전 사전조사 정보를 저장합니다.")
      ResponseEntity<ApiResponse<TenantPreContractDTO>> saveTenantInfo(TenantPreContractDTO tenantPreContractDTO);
}
