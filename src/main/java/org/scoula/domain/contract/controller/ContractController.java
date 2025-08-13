package org.scoula.domain.contract.controller;

import org.scoula.domain.contract.dto.*;
import org.scoula.global.auth.dto.CustomUserDetails;
import org.scoula.global.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(tags = "계약서 API", description = "계약서 : 정보확인 / 금액 조율 / 적법성 확인")
public interface ContractController {

      // step 1 (init)
      @ApiOperation(value = "[계약전_임차인] 계약서를 몽고DB에 저장", notes = "계약서에 필요한 항목들을 가져와서 몽고 DB에 계약서 만들기")
      ResponseEntity<ApiResponse<Void>> saveContractMongo(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails);

      // step 1 : start
      @ApiOperation(value = "[계약서 _ 정보 조회 1] 계약서 전체 조회", notes = "계약서 가져오기")
      ResponseEntity<ApiResponse<ContractDTO>> getContract(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails);

      @ApiOperation(value = "[채팅 _ 정보 조회 1] 정보 조회 시작", notes = "정보조회 마지막 단계에서 다음 단계로 넘어가기 Message")
      ResponseEntity<ApiResponse<Void>> getContractNext(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails);

      // step 1 : finish
      @ApiOperation(value = "[채팅 _ 정보 조회 2] 정보 조회에서 다음단계로 가기", notes = "다음 단계 여부(true/false)를 받아서 다음 단계로 넘어가기")
      ResponseEntity<ApiResponse<Boolean>> nextStep(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @RequestBody NextStepDTO dto);

      @ApiOperation(value = "[채팅 _ 금액 조회 1]", notes = "금액을 조율하기 위해 금액을 조회")
      ResponseEntity<ApiResponse<PaymentDTO>> getDepositPrice(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails);

      @ApiOperation(value = "[채팅 _ 금액 요청 2]", notes = "임대인이 금액을 요청")
      ResponseEntity<ApiResponse<Void>> saveDepositPrice(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @RequestBody PaymentDTO dto);

      @ApiOperation(value = "[채팅 _ 금액 거절 3]", notes = "임차인이 금액을 거절")
      ResponseEntity<ApiResponse<Void>> deleteDepositPrice(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails);

      @ApiOperation(value = "[채팅 _ 금액 수락 4]", notes = "임대인과 임차인 모두 동의")
      ResponseEntity<ApiResponse<Void>> updateDepositPrice(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails);

      @ApiOperation(value = "AI 적법성 확인 from 몽고DB", notes = "몽고DB에 있는 계약서를 AI로 보내고, 적법성 받기")
      ResponseEntity<ApiResponse<LegalityDTO>> getLegality(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails);

      @ApiOperation(value = "특약을 계약서 DB에 저장 FROM 몽고DB", notes = "특약 테이블에 있는걸 계약서로 가져오기")
      ResponseEntity<ApiResponse<Void>> saveSpecialContract(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails);

      @ApiOperation(value = "바뀐 특약 수정 from 몽고DB", notes = "적법성 검사 후 수정된 특약으로 변경")
      ResponseEntity<ApiResponse<Void>> updateSpecialContract(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @RequestBody SpecialContractUpdateDTO dto);

      @ApiOperation(value = "적법성 검사 후 다음단계로 넘어가기", notes = "적법성 검사 후 AI 메세지를 보낸다")
      ResponseEntity<ApiResponse<Void>> sendStep4(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails);
}
