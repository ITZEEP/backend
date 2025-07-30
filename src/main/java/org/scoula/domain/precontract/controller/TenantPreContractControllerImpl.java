package org.scoula.domain.precontract.controller;

import org.scoula.domain.precontract.dto.tenant.*;
import org.scoula.domain.precontract.service.PreContractService;
import org.scoula.global.auth.dto.CustomUserDetails;
import org.scoula.global.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pre-contract/{contractChatId}/buyer") // /api/도메인명
@Log4j2
public class TenantPreContractControllerImpl implements TenantPreContractController {

      private final PreContractService service;

      // =============== 사기 위험도 확인 & 기본 세팅 ==================

      @Override
      @GetMapping("/check-risk")
      public ResponseEntity<ApiResponse<Boolean>> getCheckRisk(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails) {
          return ResponseEntity.ok(
                  ApiResponse.success(service.getCheckRisk(contractChatId, userDetails.getUserId())));
      }

      // =============== 본인인증 하기 ==================

      @Override
      @PostMapping("/init-con")
      public ResponseEntity<ApiResponse<TenantInitRespDTO>> saveTenantInfo(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails) {
          return ResponseEntity.ok(
                  ApiResponse.success(
                          service.saveTenantInfo(contractChatId, userDetails.getUserId())));
          //            return
          // ResponseEntity.ok(ApiResponse.success(service.saveTenantInfo(contractChatId,
          // userDetails.getUserId())))
      }

      // =============== step 1 ==================

      @Override
      @PatchMapping("/step1")
      public ResponseEntity<ApiResponse<Void>> updateTenantStep1(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @RequestBody TenantStep1DTO step1DTO) {
          return ResponseEntity.ok(
                  ApiResponse.success(
                          service.updateTenantStep1(
                                  contractChatId, userDetails.getUserId(), step1DTO)));
      }

      @Override
      @GetMapping("/step1")
      public ResponseEntity<ApiResponse<TenantStep1DTO>> selectTenantStep1(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails) {
          return ResponseEntity.ok(
                  ApiResponse.success(
                          service.selectTenantStep1(contractChatId, userDetails.getUserId())));
      }

      // =============== step 2 ==================
      @Override
      @PatchMapping("/step2")
      public ResponseEntity<ApiResponse<Void>> updateTenantStep2(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @RequestBody TenantStep2DTO step2DTO) {
          return ResponseEntity.ok(
                  ApiResponse.success(
                          service.updateTenantStep2(
                                  contractChatId, userDetails.getUserId(), step2DTO)));
      }

      @Override
      @GetMapping("/step2")
      public ResponseEntity<ApiResponse<TenantStep2DTO>> selectTenantStep2(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails) {
          return ResponseEntity.ok(
                  ApiResponse.success(
                          service.selectTenantStep2(contractChatId, userDetails.getUserId())));
      }

      // =============== step 3 ==================

      @Override
      @PatchMapping("/step3")
      public ResponseEntity<ApiResponse<Void>> updateTenantStep3(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @RequestBody TenantStep3DTO step3DTO) {
          return ResponseEntity.ok(
                  ApiResponse.success(
                          service.updateTenantStep3(
                                  contractChatId, userDetails.getUserId(), step3DTO)));
      }

      @Override
      @GetMapping("/step3")
      public ResponseEntity<ApiResponse<TenantStep3DTO>> selectTenantStep3(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails) {
          return ResponseEntity.ok(
                  ApiResponse.success(
                          service.selectTenantStep3(contractChatId, userDetails.getUserId())));
      }

      // =============== 최종 ==================

      @Override
      @GetMapping("/pre-con")
      public ResponseEntity<ApiResponse<TenantPreContractDTO>> selectTenantPreCon(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails) {
          return ResponseEntity.ok(
                  ApiResponse.success(
                          service.selectTenantPreCon(contractChatId, userDetails.getUserId())));
      }

      @Override
      @PostMapping("/save-mongo")
      public ResponseEntity<ApiResponse<Void>> saveMongoDB(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails) {
          return ResponseEntity.ok(
                  ApiResponse.success(service.saveMongoDB(contractChatId, userDetails.getUserId())));
      }
}
