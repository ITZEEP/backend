package org.scoula.domain.precontract.controller;

import java.util.Optional;

import org.scoula.domain.chat.exception.ChatErrorCode;
import org.scoula.domain.precontract.dto.owner.*;
import org.scoula.domain.precontract.service.OwnerPreContractService;
import org.scoula.domain.user.service.UserServiceImpl;
import org.scoula.domain.user.vo.User;
import org.scoula.global.common.dto.ApiResponse;
import org.scoula.global.common.exception.BusinessException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pre-contract/{contractChatId}/owner")
@Log4j2
public class OwnerPreContractControllerImpl implements OwnerPreContractController {

      private final OwnerPreContractService ownerPreContractService;
      private final UserServiceImpl userService;

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
