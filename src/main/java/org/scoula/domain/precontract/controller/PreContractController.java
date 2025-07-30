package org.scoula.domain.precontract.controller;

import org.scoula.domain.precontract.dto.*;
import org.scoula.global.auth.dto.CustomUserDetails;
import org.scoula.global.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(tags = "계약전 사전조사 API", description = "임대인/임차인 사전조사")
public interface PreContractController {

      // =============== 사기 위험도 확인 & 기본 세팅 ==================

      @ApiOperation(value = "임차인 : step0 사기 위험도 조사 확인", notes = "임차인 사전조사 전 사기 위험도 조사 여부를 확인합니다")
      ResponseEntity<ApiResponse<Boolean>> getCheckRisk(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails);

      // =============== 본인인증 하기 ==================

      @ApiOperation(value = "임차인 : step0 데이터베이스 기본 세팅", notes = "임차인 사전조사 전 데이터베이스 행에 기본값을 넣어두기")
      ResponseEntity<ApiResponse<TenantInitRespDTO>> saveTenantInfo(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails);

      // =============== step 1 ==================

      @ApiOperation(value = "임차인 : step1 페이지 저장", notes = "임차인의 계약전 조사 step1을 데이터베이스에 저장합니다.")
      ResponseEntity<ApiResponse<Void>> updateTenantStep1(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @RequestBody TenantStep1DTO step1DTO);

      @ApiOperation(value = "임차인 : step1 페이지 조회", notes = "임차인의 계약전 조사 step1을 조회합니다.")
      ResponseEntity<ApiResponse<TenantStep1DTO>> selectTenantStep1(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails);

      // =============== step 2 ==================

      @ApiOperation(value = "임차인 : step2 페이지 저장", notes = "임차인의 계약전 조사 step2을 데이터베이스에 저장합니다.")
      ResponseEntity<ApiResponse<Void>> updateTenantStep2(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @RequestBody TenantStep2DTO step2DTO);

      @ApiOperation(value = "임차인 : step2 페이지 조회", notes = "임차인의 계약전 조사 step2을 조회합니다.")
      ResponseEntity<ApiResponse<TenantStep2DTO>> selectTenantStep2(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails);

      // =============== step 3 ==================
      @ApiOperation(value = "임차인 : step3 페이지 저장", notes = "임차인의 계약전 조사 step3을 데이터베이스에 저장합니다.")
      ResponseEntity<ApiResponse<Void>> updateTenantStep3(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @RequestBody TenantStep3DTO step3DTO);

      @ApiOperation(value = "임차인 : step3 페이지 조회", notes = "임차인의 계약전 조사 step3을 조회합니다.")
      ResponseEntity<ApiResponse<TenantStep3DTO>> selectTenantStep3(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails);

      // =============== 최종 ==================
      @ApiOperation(value = "임차인 : step4 최종 페이지 조회", notes = "임차인의 계약전 조사 최종 페이지를 조회합니다.")
      ResponseEntity<ApiResponse<TenantPreContractDTO>> selectTenantPreCon(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails);

      @ApiOperation(value = "임차인 : step5 몽고DB에 저장", notes = "임차인의 계약전 작성 사항을 AI로 보내기 위해 몽고DB에 저장한다.")
      ResponseEntity<ApiResponse<Void>> saveMongoDB(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails);
}
