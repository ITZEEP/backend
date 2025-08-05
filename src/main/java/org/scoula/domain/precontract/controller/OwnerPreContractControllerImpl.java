package org.scoula.domain.precontract.controller;

import java.util.Optional;

import org.scoula.domain.chat.exception.ChatErrorCode;
import org.scoula.domain.precontract.document.ContractDocumentMongoDocument;
import org.scoula.domain.precontract.dto.ai.ContractParseResponseDto;
import org.scoula.domain.precontract.dto.owner.*;
import org.scoula.domain.precontract.service.OwnerPreContractService;
import org.scoula.domain.user.service.UserServiceImpl;
import org.scoula.domain.user.vo.User;
import org.scoula.global.common.dto.ApiResponse;
import org.scoula.global.common.exception.BusinessException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pre-contract/{contractChatId}/owner")
@Log4j2
public class OwnerPreContractControllerImpl implements OwnerPreContractController {

      private final OwnerPreContractService ownerPreContractService;
      private final UserServiceImpl userService;

      // =============== 데이터베이스 세팅 ==================
      @Override
      @PostMapping("/save")
      public ResponseEntity<ApiResponse<String>> saveOwnerInfo(
              @PathVariable Long contractChatId, Authentication authentication) {
          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
          }

          User currentUser = currentUserOpt.get();
          Long userId = currentUser.getUserId();
          ownerPreContractService.saveOwnerInfo(contractChatId, userId);
          return ResponseEntity.ok(ApiResponse.success("저장 완료"));
      }

      // =============== 계약 정보 설정 Step1 ==================
      // 저장
      @Override
      @PatchMapping("/contract-step1")
      public ResponseEntity<ApiResponse<Boolean>> updateOwnerContractStep1(
              @PathVariable Long contractChatId,
              Authentication authentication,
              @RequestBody OwnerContractStep1DTO contractStep1DTO) {
          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
          }

          User currentUser = currentUserOpt.get();
          Long userId = currentUser.getUserId();
          ownerPreContractService.updateOwnerContractStep1(contractChatId, userId, contractStep1DTO);
          return ResponseEntity.ok(ApiResponse.success(true));
      }

      // 조회
      @Override
      @GetMapping("/contract-step1")
      public ResponseEntity<ApiResponse<OwnerContractStep1DTO>> selectOwnerContractStep1(
              @PathVariable Long contractChatId, Authentication authentication) {
          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
          }

          User currentUser = currentUserOpt.get();
          Long userId = currentUser.getUserId();
          OwnerContractStep1DTO dto =
                  ownerPreContractService.selectOwnerContractStep1(contractChatId, userId);
          return ResponseEntity.ok(ApiResponse.success(dto));
      }

      // =============== 계약 정보 설정 Step 2 ==================
      // 저장
      @Override
      @PatchMapping("/contract-step2")
      public ResponseEntity<ApiResponse<Boolean>> updateOwnerContractStep2(
              @PathVariable Long contractChatId,
              Authentication authentication,
              @RequestBody OwnerContractStep2DTO contractStep2DTO) {
          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
          }

          User currentUser = currentUserOpt.get();
          Long userId = currentUser.getUserId();
          ownerPreContractService.updateOwnerContractStep2(contractChatId, userId, contractStep2DTO);
          return ResponseEntity.ok(ApiResponse.success(true));
      }

      // 조회
      @Override
      @GetMapping("/contract-step2")
      public ResponseEntity<ApiResponse<OwnerContractStep2DTO>> selectOwnerContractStep2(
              @PathVariable Long contractChatId, Authentication authentication) {
          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
          }

          User currentUser = currentUserOpt.get();
          Long userId = currentUser.getUserId();
          OwnerContractStep2DTO dto =
                  ownerPreContractService.selectOwnerContractStep2(contractChatId, userId);
          return ResponseEntity.ok(ApiResponse.success(dto));
      }

      // =============== 거주 정보 설정 ==================
      // 저장
      @Override
      @PatchMapping("/living-step1")
      public ResponseEntity<ApiResponse<Boolean>> updateOwnerLivingStep1(
              @PathVariable Long contractChatId,
              Authentication authentication,
              @RequestBody OwnerLivingStep1DTO livingStep1DTO) {
          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
          }

          User currentUser = currentUserOpt.get();
          Long userId = currentUser.getUserId();
          ownerPreContractService.updateOwnerLivingStep1(contractChatId, userId, livingStep1DTO);
          return ResponseEntity.ok(ApiResponse.success(true));
      }

      // 조회
      @Override
      @GetMapping("/living-step1")
      public ResponseEntity<ApiResponse<OwnerLivingStep1DTO>> selectOwnerLivingStep1(
              @PathVariable Long contractChatId, Authentication authentication) {
          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
          }

          User currentUser = currentUserOpt.get();
          Long userId = currentUser.getUserId();
          OwnerLivingStep1DTO dto =
                  ownerPreContractService.selectOwnerLivingStep1(contractChatId, userId);
          return ResponseEntity.ok(ApiResponse.success(dto));
      }

      // =============== 계약서 특약 내용 ==================
      // 조회
      @Override
      @GetMapping("/contract-document")
      public ResponseEntity<ApiResponse<ContractDocumentMongoDocument>> getContractDocument(
              @PathVariable Long contractChatId, Authentication authentication) {
          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
          }

          User currentUser = currentUserOpt.get();
          Long userId = currentUser.getUserId();

          ContractDocumentMongoDocument document =
                  ownerPreContractService.getContractDocument(contractChatId, userId);

          return ResponseEntity.ok(ApiResponse.success(document));
      }

      // OCR 분석
      @Override
      @PostMapping(value = "/analyze-contract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
      public ResponseEntity<ApiResponse<ContractParseResponseDto>> analyzeContractDocument(
              @PathVariable Long contractChatId,
              Authentication authentication,
              @RequestPart("file") MultipartFile file) {
          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
          }

          User currentUser = currentUserOpt.get();
          Long userId = currentUser.getUserId();
          ContractParseResponseDto result = ownerPreContractService.analyzeContractDocument(file);
          return ResponseEntity.ok(ApiResponse.success(result));
      }

      // 저장
      @Override
      @PostMapping("/save-contract")
      public ResponseEntity<ApiResponse<Boolean>> saveContractDocument(
              @PathVariable Long contractChatId,
              Authentication authentication,
              @RequestBody ContractDocumentDTO contractDocumentDTO) {
          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
          }

          User currentUser = currentUserOpt.get();
          Long userId = currentUser.getUserId();
          ownerPreContractService.saveContractDocument(contractChatId, userId, contractDocumentDTO);
          return ResponseEntity.ok(ApiResponse.success(true));
      }

      // =============== 최종 정보 확인 ==================
      @Override
      @GetMapping("/summary")
      public ResponseEntity<ApiResponse<OwnerPreContractDTO>> selectOwnerContract(
              @PathVariable Long contractChatId, Authentication authentication) {
          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
          }

          User currentUser = currentUserOpt.get();
          Long userId = currentUser.getUserId();
          OwnerPreContractDTO dto =
                  ownerPreContractService.selectOwnerPreContract(contractChatId, userId);
          return ResponseEntity.ok(ApiResponse.success(dto));
      }

      // =============== 최종 정보 MongoDB로 저장 ==================
      @Override
      @PostMapping("/save-mongo")
      public ResponseEntity<ApiResponse<Void>> saveMongoDB(
              @PathVariable Long contractChatId, Authentication authentication) {
          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
          }

          User currentUser = currentUserOpt.get();
          Long userId = currentUser.getUserId();

          ownerPreContractService.saveMongoDB(contractChatId, userId);
          return ResponseEntity.ok(ApiResponse.success(null));
      }
}
