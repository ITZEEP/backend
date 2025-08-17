package org.scoula.domain.contract.controller;

import java.util.Map;

import org.scoula.domain.chat.dto.FinalContractDeletionResponseDto;
import org.scoula.domain.contract.dto.*;
import org.scoula.global.auth.dto.CustomUserDetails;
import org.scoula.global.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(tags = "계약서 API", description = "계약서 : 정보확인 / 금액 조율 / 적법성 확인")
public interface ContractController {

      // 대기
      @ApiOperation(
              value = "[대기] 계약전_임차인 | 계약서를 몽고DB에 저장",
              notes = "계약서에 필요한 항목들을 가져와서 몽고 DB에 계약서 만들기")
      ResponseEntity<ApiResponse<Void>> saveContractMongo(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails);

      // 정보 조회
      @ApiOperation(value = "[정보 조회] 계약서 1 | 계약서 전체 조회", notes = "계약서 가져오기")
      ResponseEntity<ApiResponse<ContractDTO>> getContract(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails);

      @ApiOperation(value = "[정보 조회] 채팅 1 | 정보 조회 시작", notes = "정보조회 마지막 단계에서 다음 단계로 넘어가기 Message")
      ResponseEntity<ApiResponse<Void>> getContractNext(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails);

      @ApiOperation(
              value = "[정보 조회] 채팅 2 | 정보 조회에서 다음단계로 가기",
              notes = "다음 단계 여부(true/false)를 받아서 다음 단계로 넘어가기")
      ResponseEntity<ApiResponse<Boolean>> nextStep(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @RequestBody NextStepDTO dto);

      // 금액 조정
      @ApiOperation(value = "[금액 조정] 채팅 1 | 금액 조회", notes = "금액을 조율하기 위해 금액을 조회")
      ResponseEntity<ApiResponse<PaymentDTO>> getDepositPrice(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails);

      @ApiOperation(value = "[금액 조정] 채팅 2 | 금액 요청 ", notes = "임대인이 금액을 요청")
      ResponseEntity<ApiResponse<Void>> saveDepositPrice(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @RequestBody PaymentDTO dto);

      @ApiOperation(value = "[금액 조정] 채팅 3 | 금액 거절", notes = "임차인이 금액을 거절")
      ResponseEntity<ApiResponse<Void>> deleteDepositPrice(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails);

      @ApiOperation(value = "[금액 조정] 채팅 4 | 금액 수락", notes = "임대인과 임차인 모두 동의")
      ResponseEntity<ApiResponse<Void>> updateDepositPrice(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails);

      //      // 적법성 검사
      //      @ApiOperation(value = "??? 몽고 디비랑 합친것 [적법성 검사] 계약서 1 | 계약서 전체 조회", notes = "계약서 가져오기")
      //      ResponseEntity<ApiResponse<ContractDTO>> getContracts(
      //              @PathVariable Long contractChatId,
      //              @AuthenticationPrincipal CustomUserDetails userDetails);

      @ApiOperation(value = "[적법성 검사] 계약서 1 몽고DB에 특약 저장 ", notes = "몽고DB에 특약 저장하기 ")
      ResponseEntity<ApiResponse<Void>> saveSpecialContract(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails);

      @ApiOperation(
              value = "[적법성 검사] 채팅 1 | AI 적법성 확인 from 몽고DB ",
              notes = "몽고DB에 있는 계약서를 AI로 보내고, 적법성 받기")
      ResponseEntity<ApiResponse<LegalityDTO>> getLegality(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails);

      @ApiOperation(value = "[적법성 검사] 채팅 2  | 임대인 수정", notes = "임대인 : 적법성 검사 수정")
      ResponseEntity<ApiResponse<Void>> updateOwnerLegality(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @RequestBody UpdateLegalityDTO dto);

      @ApiOperation(value = "[적법성 검사] 채팅 3 | 임대인 삭제", notes = "임대인 : 적법성 검사 삭제")
      ResponseEntity<ApiResponse<String>> deleteOwnerLegality(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails);

      @ApiOperation(value = "[적법성 검사] 채팅 4 | 임차인 수정", notes = "임차인 : 적법성 검사 수정 완료")
      ResponseEntity<ApiResponse<Void>> updateBuyerLegality(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @RequestBody SpecialContractUpdateDTO dto);

      @ApiOperation(value = "[적법성 검사] 채팅 5 | 임차인 거절", notes = "임차인 : 적법성 검사 거절")
      ResponseEntity<ApiResponse<String>> rejectBuyerLegality(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails);

      @ApiOperation(value = "[적법성 검사} 채팅 6 | 적법성 검사 후 다음단계로 넘어가기", notes = "내보내기 단계로 넘어가기")
      ResponseEntity<ApiResponse<Void>> sendStep4(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails);

      // 메서드만 만들어서 써도 될 듯
      @ApiOperation(value = "바뀐 특약 수정 from 몽고DB", notes = "적법성 검사 후 수정된 특약으로 변경")
      ResponseEntity<ApiResponse<Void>> updateSpecialContract(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @RequestBody SpecialContractUpdateDTO dto);

      @ApiOperation(value = "최종 계약서 확정 요청 (임대인)", notes = "임대인이 최종 특약서에 대한 확정을 요청합니다.")
      ResponseEntity<ApiResponse<String>> requestFinalContract(
              @PathVariable Long contractChatId, Authentication authentication);

      @ApiOperation(value = "최종 계약서 확정 수락 (임차인)", notes = "임차인이 임대인의 최종 특약서 확정 요청을 수락합니다.")
      ResponseEntity<ApiResponse<Map<String, Object>>> acceptFinalContract(
              @PathVariable Long contractChatId,
              @RequestBody FinalContractDeletionResponseDto responseDto,
              Authentication authentication);
}
