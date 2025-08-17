package org.scoula.domain.contract.controller;

import org.scoula.domain.chat.service.ContractChatServiceInterface;
import org.scoula.domain.contract.dto.*;
import org.scoula.domain.contract.service.ContractFixServiceInterface;
import org.scoula.domain.contract.service.ContractService;
import org.scoula.global.auth.dto.CustomUserDetails;
import org.scoula.global.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@Log4j2
@RequiredArgsConstructor
@RequestMapping("/api/contract/{contractChatId}")
public class ContractControllerImpl implements ContractController {

      private final ContractFixServiceInterface contractFixService;

      private final ContractService service;
      private final ContractChatServiceInterface contractChatService;

      @Override
      @PostMapping("")
      public ResponseEntity<ApiResponse<Void>> saveContractMongo(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails) {
          return ResponseEntity.ok(
                  ApiResponse.success(
                          service.saveContractMongo(contractChatId, userDetails.getUserId())));
      }

      @Override
      @PostMapping("/getContract")
      public ResponseEntity<ApiResponse<ContractDTO>> getContract(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails) {
          return ResponseEntity.ok(
                  ApiResponse.success(service.getContract(contractChatId, userDetails.getUserId())));
      }

      @Override
      @PostMapping("/step1")
      public ResponseEntity<ApiResponse<Void>> getContractNext(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails) {
          return ResponseEntity.ok(
                  ApiResponse.success(
                          service.getContractNext(contractChatId, userDetails.getUserId())));
      }

      @Override
      @PostMapping("/nextStep")
      public ResponseEntity<ApiResponse<Boolean>> nextStep(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @RequestBody NextStepDTO dto) {
          return ResponseEntity.ok(
                  ApiResponse.success(
                          service.nextStep(contractChatId, userDetails.getUserId(), dto)));
      }

      @Override
      @PostMapping("/getPrice")
      public ResponseEntity<ApiResponse<PaymentDTO>> getDepositPrice(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails) {
          return ResponseEntity.ok(
                  ApiResponse.success(
                          service.getDepositPrice(contractChatId, userDetails.getUserId())));
      }

      @Override
      @PostMapping("/price")
      public ResponseEntity<ApiResponse<Void>> saveDepositPrice(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @RequestBody PaymentDTO dto) {
          return ResponseEntity.ok(
                  ApiResponse.success(
                          service.saveDepositPrice(contractChatId, userDetails.getUserId(), dto)));
      }

      @Override
      @DeleteMapping("/price")
      public ResponseEntity<ApiResponse<Void>> deleteDepositPrice(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails) {
          return ResponseEntity.ok(
                  ApiResponse.success(
                          service.deleteDepositPrice(contractChatId, userDetails.getUserId())));
      }

      @Override
      @PatchMapping("/price")
      public ResponseEntity<ApiResponse<Void>> updateDepositPrice(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails) {
          return ResponseEntity.ok(
                  ApiResponse.success(
                          service.updateDepositPrice(contractChatId, userDetails.getUserId())));
      }

      @Override
      @PostMapping("/save/special-contract")
      public ResponseEntity<ApiResponse<Void>> saveSpecialContract(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails) {
          return ResponseEntity.ok(
                  ApiResponse.success(
                          contractFixService.saveSpecialContract(
                                  contractChatId, userDetails.getUserId())));
      }

      @Override
      @PostMapping("/legality")
      public ResponseEntity<ApiResponse<LegalityDTO>> getLegality(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails) {
          return ResponseEntity.ok(
                  ApiResponse.success(
                          contractFixService.getLegality(contractChatId, userDetails.getUserId())));
      }

      @Override
      @DeleteMapping("/delete/legality")
      public ResponseEntity<ApiResponse<String>> deleteOwnerLegality(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails) {
          return ResponseEntity.ok(
                  ApiResponse.success(
                          service.deleteOwnerLegality(contractChatId, userDetails.getUserId())));
      }

      @Override
      @PostMapping("/suggest/legality")
      public ResponseEntity<ApiResponse<Void>> updateOwnerLegality(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @RequestBody UpdateLegalityDTO dto) {
          return ResponseEntity.ok(
                  ApiResponse.success(
                          service.updateOwnerLegality(contractChatId, userDetails.getUserId(), dto)));
      }

      @Override
      @PostMapping("/update/legality")
      public ResponseEntity<ApiResponse<Void>> updateBuyerLegality(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @RequestBody SpecialContractUpdateDTO dto) {
          return ResponseEntity.ok(
                  ApiResponse.success(
                          service.updateBuyerLegality(contractChatId, userDetails.getUserId(), dto)));
      }

      @Override
      @GetMapping("/reject/legality")
      public ResponseEntity<ApiResponse<String>> rejectBuyerLegality(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails) {
          return ResponseEntity.ok(
                  ApiResponse.success(
                          service.rejectBuyerLegality(contractChatId, userDetails.getUserId())));
      }

      @Override
      @PatchMapping("/specialContract")
      public ResponseEntity<ApiResponse<Void>> updateSpecialContract(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @RequestBody SpecialContractUpdateDTO dto) {
          return ResponseEntity.ok(
                  ApiResponse.success(
                          service.updateSpecialContract(
                                  contractChatId, userDetails.getUserId(), dto)));
      }

      @Override
      @GetMapping("/specialContract")
      public ResponseEntity<ApiResponse<Void>> sendStep4(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails) {
          return ResponseEntity.ok(
                  ApiResponse.success(service.sendStep4(contractChatId, userDetails.getUserId())));
      }
}
