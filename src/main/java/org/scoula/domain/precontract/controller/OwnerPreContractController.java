package org.scoula.domain.precontract.controller;

import org.scoula.domain.precontract.document.ContractDocumentMongoDocument;
import org.scoula.domain.precontract.dto.owner.ContractDocumentDTO;
import org.scoula.domain.precontract.dto.owner.OwnerContractStep1DTO;
import org.scoula.domain.precontract.dto.owner.OwnerContractStep2DTO;
import org.scoula.domain.precontract.dto.owner.OwnerLivingStep1DTO;
import org.scoula.domain.precontract.dto.owner.OwnerPreContractDTO;
import org.scoula.global.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(tags = "임대인 계약 사전조사 API", description = "임대인 계약 작성 전 사전 조사")
public interface OwnerPreContractController {
      // =============== 데이터베이스 세팅 ==================
      @ApiOperation(value = "임대인 : step0 데이터베이스 기본 세팅", notes = "임대인 사전조사 전 데이터베이스 행에 기본값을 넣어두기")
      ResponseEntity<ApiResponse<String>> saveOwnerInfo(
              @PathVariable Long contractChatId, Authentication authentication);

      // =============== 계약 정보 설정 Step1 ==================
      @ApiOperation(
              value = "임대인 : 계약 정보 설정 step 1 페이지 저장",
              notes = "임대인 사전조사 계약 정보 설정 step1을 데이터베이스에 저장합니다.")
      ResponseEntity<ApiResponse<Boolean>> updateOwnerContractStep1(
              @PathVariable Long contractChatId,
              Authentication authentication,
              @RequestBody OwnerContractStep1DTO contractStep1DTO);

      @ApiOperation(
              value = "임대인 : 계약 정보 설정 step 1 페이지 조회",
              notes = "임대인 사전조사 계약 정보 설정 step 1을 조회합니다.")
      ResponseEntity<ApiResponse<OwnerContractStep1DTO>> selectOwnerContractStep1(
              @PathVariable Long contractChatId, Authentication authentication);

      // =============== 계약 정보 설정 Step 2 ==================
      @ApiOperation(
              value = "임대인 : 계약 정보 설정 step 2 페이지 저장",
              notes = "임대인 사전조사 계약 정보 설정 step2을 데이터베이스에 저장합니다.")
      ResponseEntity<ApiResponse<Boolean>> updateOwnerContractStep2(
              @PathVariable Long contractChatId,
              Authentication authentication,
              @RequestBody OwnerContractStep2DTO contractStep2DTO);

      @ApiOperation(
              value = "임대인 : 계약 정보 설정 step 2 페이지 조회",
              notes = "임대인 사전조사 계약 정보 설정 step 2을 조회합니다.")
      ResponseEntity<ApiResponse<OwnerContractStep2DTO>> selectOwnerContractStep2(
              @PathVariable Long contractChatId, Authentication authentication);

      // =============== 거주 정보 설정 Step 1 ==================
      @ApiOperation(
              value = "임대인 : 거주 정보 설정 step 1 페이지 저장",
              notes = "임대인 사전조사 거주 정보 설정 step 1을 데이터베이스에 저장합니다.")
      ResponseEntity<ApiResponse<Boolean>> updateOwnerLivingStep1(
              @PathVariable Long contractChatId,
              Authentication authentication,
              @RequestBody OwnerLivingStep1DTO contractStep2DTO);

      @ApiOperation(
              value = "임대인 : 거주 정보 설정 step 1 페이지 조회",
              notes = "임대인 사전조사 거주 정보 설정 step 1을 조회합니다.")
      ResponseEntity<ApiResponse<OwnerLivingStep1DTO>> selectOwnerLivingStep1(
              @PathVariable Long contractChatId, Authentication authentication);

      // =============== 계약서 특약 내용 ==================
      @ApiOperation(value = "임대인 : 계약서 특약 내용 몽고DB 조회", notes = "OCR로 추출된 특약 내용을 조회합니다.")
      ResponseEntity<ApiResponse<ContractDocumentMongoDocument>> getContractDocument(
              @PathVariable Long contractChatId, Authentication authentication);

      @ApiOperation(value = "임대인 : 계약서 특약 내용 몽고DB에 저장", notes = "OCR로 추출된 특약 내용을 저장합니다.")
      ResponseEntity<ApiResponse<Boolean>> saveContractDocument(
              @PathVariable Long contractChatId,
              Authentication authentication,
              @RequestBody ContractDocumentDTO contractDocumentDTO);

      // =============== 최종 정보 확인 ==================
      @ApiOperation(value = "임대인 : 최종 페이지 조회", notes = "임대인의 계약전 조사 최종 페이지를 조회합니다.")
      ResponseEntity<ApiResponse<OwnerPreContractDTO>> selectOwnerContract(
              @PathVariable Long contractChatId, Authentication authentication);

      // =============== 최종 정보 MongoDB로 저장 ==================
      @ApiOperation(value = "임대인 : 최종 데이터 몽고DB에 저장", notes = "임대인의 계약전 작성 사항을 AI로 보내기 위해 몽고DB에 저장한다.")
      ResponseEntity<ApiResponse<Void>> saveMongoDB(
              @PathVariable Long contractChatId, Authentication authentication);
}
