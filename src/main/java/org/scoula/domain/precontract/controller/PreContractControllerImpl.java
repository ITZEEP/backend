package org.scoula.domain.precontract.controller;

import org.scoula.domain.precontract.dto.TenantPreContractDTO;
import org.scoula.domain.precontract.service.PreContractService;
import org.scoula.global.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/precon") // /api/도메인명
@Log4j2
public class PreContractControllerImpl implements PreContractController {

      private final PreContractService service;

      @Override
      @PostMapping
      public ResponseEntity<ApiResponse<TenantPreContractDTO>> saveTenantInfo(@RequestBody TenantPreContractDTO tenantPreContractDTO) {
          return ResponseEntity.ok(ApiResponse.success(service.saveTenantInfo(tenantPreContractDTO)));
      }
}
