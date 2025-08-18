package org.scoula.domain.contract.controller;

import javax.servlet.http.HttpServletResponse;

import java.util.Map;
import java.util.Optional;

import org.scoula.domain.chat.dto.FinalContractDeletionResponseDto;
import org.scoula.domain.chat.exception.ChatErrorCode;
import org.scoula.domain.chat.service.ContractChatServiceInterface;
import org.scoula.domain.contract.dto.*;
import org.scoula.domain.contract.service.ContractFixServiceInterface;
import org.scoula.domain.contract.service.ContractService;
import org.scoula.domain.user.service.UserServiceInterface;
import org.scoula.domain.user.vo.User;
import org.scoula.global.auth.dto.CustomUserDetails;
import org.scoula.global.common.dto.ApiResponse;
import org.scoula.global.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@Log4j2
@RequiredArgsConstructor
@RequestMapping("/api/contract/{contractChatId}")
public class ContractControllerImpl implements ContractController {

      private final ContractFixServiceInterface contractFixService;
      private final UserServiceInterface userService;
      private final ContractService service;
      private final ContractChatServiceInterface contractChatService;

      private Long getUserIdFromAuthentication(Authentication authentication) {
          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
          }

          return currentUserOpt.get().getUserId();
      }

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
      @PostMapping("/price/request")
      public ResponseEntity<ApiResponse<Void>> saveDepositPrice(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @RequestBody PaymentDTO dto) {
          return ResponseEntity.ok(
                  ApiResponse.success(
                          service.saveDepositPrice(contractChatId, userDetails.getUserId(), dto)));
      }

      @Override
      @PostMapping("/price/reject")
      public ResponseEntity<ApiResponse<Void>> deleteDepositPrice(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails) {
          return ResponseEntity.ok(
                  ApiResponse.success(
                          service.deleteDepositPrice(contractChatId, userDetails.getUserId())));
      }

      @Override
      @PatchMapping("/price/accept")
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

      @Override
      @PostMapping("/specialContract/final-request")
      public ResponseEntity<ApiResponse<String>> requestFinalContract(
              @PathVariable Long contractChatId, Authentication authentication) {

          try {
              Long userId = getUserIdFromAuthentication(authentication);
              contractChatService.requestFinalContract(contractChatId, userId);
              return ResponseEntity.ok(ApiResponse.success("최종 특약 확정 요청이 임차인에게 전송되었습니다."));

          } catch (BusinessException e) {
              return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
          } catch (Exception e) {
              log.error("최종 특약서 확정 요청 처리 중 오류 발생", e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(ApiResponse.error("서버 오류가 발생했습니다."));
          }
      }

      @Override
      @PostMapping("/specialContract/final-accept")
      public ResponseEntity<ApiResponse<Map<String, Object>>> acceptFinalContract(
              @PathVariable Long contractChatId,
              @RequestBody FinalContractDeletionResponseDto responseDto,
              Authentication authentication) {

          try {
              Long userId = getUserIdFromAuthentication(authentication);

              Map<String, Object> result =
                      contractChatService.acceptFinalContract(
                              contractChatId, userId, responseDto.isAccepted());
              return ResponseEntity.ok(ApiResponse.success(result));

          } catch (BusinessException e) {
              return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
          } catch (Exception e) {
              log.error("최종 특약서 확정 수락 처리 중 오류 발생", e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(ApiResponse.error("서버 오류가 발생했습니다."));
          }
      }

      // ========================================

      //      @Override
      //      @PostMapping("/final_contract")
      //      public ResponseEntity<ApiResponse<Void>> finalContractInit(
      //              @PathVariable Long contractChatId,
      //              @AuthenticationPrincipal CustomUserDetails userDetails) {
      //          return ResponseEntity.ok(
      //                  ApiResponse.success(
      //                          service.finalContractInit(contractChatId,
      // userDetails.getUserId())));
      //      }

      @Override
      @GetMapping("/final_contract")
      public ResponseEntity<ApiResponse<MultipartFile>> finalContractPDF(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails) {
          return ResponseEntity.ok(
                  ApiResponse.success(
                          service.finalContractPDF(contractChatId, userDetails.getUserId())));
      }

      @Override
      @PostMapping("/signature/tax")
      public ResponseEntity<ApiResponse<Boolean>> saveSignature(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails,
              //              @RequestPart("dto") SaveSignatureDTO dto, // JSON 파트
              @RequestParam("dto") String dtoText,
              @RequestPart("imgFiles") MultipartFile imgFiles)
              throws Exception {
          if (imgFiles == null || imgFiles.isEmpty()) {
              throw new IllegalArgumentException("서명 이미지가 비어 있습니다.");
          }
          // 문자열 -> DTO (JSON도, 'TAX' 같은 단일 문자열도 허용)
          SaveSignatureDTO dto;
          try {
              dto =
                      new ObjectMapper()
                              .readValue(dtoText, SaveSignatureDTO.class); // {"signedType":"TAX"}
          } catch (Exception ignore) {
              dto =
                      SaveSignatureDTO.builder()
                              .signedType(
                                      org.scoula.domain.contract.enums.SignedType.valueOf(
                                              dtoText.trim().toUpperCase()))
                              .build(); // TAX
          }
          return ResponseEntity.ok(
                  ApiResponse.success(
                          service.saveSignature(
                                  contractChatId, userDetails.getUserId(), dto, imgFiles)));
      }

      @Override
      @PostMapping("/finalContract/p")
      public ResponseEntity<ApiResponse<byte[]>> saveFinalContract(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @RequestBody ContractPasswordDTO dto) {
          return ResponseEntity.ok(
                  ApiResponse.success(
                          service.saveFinalContract(contractChatId, userDetails.getUserId(), dto)));
      }

      @Override
      @GetMapping("/finalContract")
      public ResponseEntity<ApiResponse<byte[]>> selectContractPDF(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetail,
              @RequestBody ContractPasswordDTO dto) {
          return ResponseEntity.ok(
                  ApiResponse.success(
                          service.selectContractPDF(contractChatId, userDetail.getUserId(), dto)));
      }

      //      @Override
      //      @PostMapping("/pdf")
      //      public ResponseEntity<ApiResponse<Void>> saveContractPDF(
      //              @PathVariable Long contractChatId,
      //              @AuthenticationPrincipal CustomUserDetails userDetails,
      //              @RequestBody FinalContractDTO dto) {
      //          return ResponseEntity.ok(
      //                  ApiResponse.success(
      //                          service.saveContractPDF(contractChatId, userDetails.getUserId(),
      // dto)));
      //      }
      //
      //      @Override
      //      @GetMapping("/signature")
      //      public ResponseEntity<ApiResponse<Void>> selectSignaturePDF(
      //              @PathVariable Long contractChatId,
      //              @AuthenticationPrincipal CustomUserDetails userDetails,
      //              HttpServletResponse response) {
      //          return ResponseEntity.ok(
      //                  ApiResponse.success(
      //                          service.selectSignaturePDF(
      //                                  contractChatId, userDetails.getUserId(), response)));
      //      }

      @Override
      @PostMapping("/pdf")
      public ResponseEntity<ApiResponse<Void>> selectContractPDF(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails,
              HttpServletResponse response,
              @RequestBody FindContractDTO dto)
              throws Exception {
          return ResponseEntity.ok(
                  ApiResponse.success(
                          service.selectContractPDF(
                                  contractChatId, userDetails.getUserId(), response, dto)));
      }

      @Override
      @PostMapping("/email")
      public ResponseEntity<ApiResponse<Void>> sendContractPDF(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @RequestBody FindContractDTO dto)
              throws Exception {
          return ResponseEntity.ok(
                  ApiResponse.success(
                          service.sendContractPDF(contractChatId, userDetails.getUserId(), dto)));
      }
}
