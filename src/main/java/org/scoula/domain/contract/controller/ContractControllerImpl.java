package org.scoula.domain.contract.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import org.scoula.domain.chat.dto.FinalContractDeletionResponseDto;
import org.scoula.domain.chat.exception.ChatErrorCode;
import org.scoula.domain.chat.service.ContractChatServiceInterface;
import org.scoula.domain.contract.dto.*;
import org.scoula.domain.contract.service.ContractExportSyncService;
import org.scoula.domain.contract.service.ContractFixServiceInterface;
import org.scoula.domain.contract.service.ContractService;
import org.scoula.domain.user.service.UserServiceInterface;
import org.scoula.domain.user.vo.User;
import org.scoula.global.auth.dto.CustomUserDetails;
import org.scoula.global.common.dto.ApiResponse;
import org.scoula.global.common.exception.BusinessException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
      private final ContractExportSyncService exportSyncService;
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
      @PostMapping("/start-export")
      public ResponseEntity<byte[]> startContractExport(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails) {
          byte[] pdfBytes = service.startContractExport(contractChatId, userDetails.getUserId());

          if (pdfBytes == null || pdfBytes.length == 0) {
              return ResponseEntity.noContent().build();
          }

          return ResponseEntity.ok()
                  .contentType(MediaType.APPLICATION_PDF)
                  .header("Content-Disposition", "inline; filename=\"contract.pdf\"")
                  .contentLength(pdfBytes.length)
                  .body(pdfBytes);
      }

      @GetMapping("/preview")
      public ResponseEntity<ApiResponse<String>> getPreviewPdf(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails) {

          try {
              // 권한 확인: 해당 계약서의 임차인/임대인인지 확인
              String userRole =
                      exportSyncService.getUserRole(contractChatId, userDetails.getUserId());
              if (!"owner".equals(userRole) && !"buyer".equals(userRole)) {
                  return ResponseEntity.status(HttpStatus.FORBIDDEN)
                          .body(ApiResponse.error("해당 계약서에 접근할 권한이 없습니다"));
              }

              // PDF 생성
              byte[] pdfBytes = service.startContractExport(contractChatId, userDetails.getUserId());

              if (pdfBytes == null || pdfBytes.length == 0) {
                  return ResponseEntity.badRequest().body(ApiResponse.error("PDF 생성에 실패했습니다"));
              }

              // S3에 임시 파일로 업로드하고 전체 URL 반환
              String fileName =
                      "contract_preview_"
                              + contractChatId
                              + "_"
                              + System.currentTimeMillis()
                              + ".pdf";
              String tempUrl = exportSyncService.uploadTempPdf(pdfBytes, fileName);

              log.info(
                      "PDF preview S3 upload successful - user: {}, role: {}, contract: {}, URL: {}",
                      userDetails.getUserId(),
                      userRole,
                      contractChatId,
                      tempUrl);

              return ResponseEntity.ok(ApiResponse.success(tempUrl));

          } catch (Exception e) {
              log.error("PDF 미리보기 생성 실패", e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(ApiResponse.error("PDF 미리보기 생성에 실패했습니다: " + e.getMessage()));
          }
      }

      @GetMapping("/preview-url")
      public ResponseEntity<ApiResponse<String>> createTempPdfUrl(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails) {

          try {
              // 권한 확인: 해당 계약서의 임차인/임대인인지 확인
              String userRole =
                      exportSyncService.getUserRole(contractChatId, userDetails.getUserId());
              if (!"owner".equals(userRole) && !"buyer".equals(userRole)) {
                  return ResponseEntity.status(HttpStatus.FORBIDDEN)
                          .body(ApiResponse.error("해당 계약서에 접근할 권한이 없습니다"));
              }

              // PDF 생성
              byte[] pdfBytes = service.startContractExport(contractChatId, userDetails.getUserId());

              if (pdfBytes == null || pdfBytes.length == 0) {
                  return ResponseEntity.badRequest().body(ApiResponse.error("PDF 생성에 실패했습니다"));
              }

              // 임시 파일명 생성 (계약서ID + 타임스탬프)
              String fileName =
                      String.format("contract_%d_%d.pdf", contractChatId, System.currentTimeMillis());

              // S3에 임시 업로드 (1시간 후 자동 삭제 설정)
              String tempUrl = exportSyncService.uploadTempPdf(pdfBytes, fileName);

              log.info(
                      "Temporary PDF URL created for user {} (role: {}) for contract {}",
                      userDetails.getUserId(),
                      userRole,
                      contractChatId);

              return ResponseEntity.ok(ApiResponse.success(tempUrl));

          } catch (Exception e) {
              log.error("임시 PDF URL 생성 실패", e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(ApiResponse.error("임시 URL 생성에 실패했습니다"));
          }
      }

      @GetMapping("/temp-pdf/{fileName}")
      public ResponseEntity<?> getTempPdf(
              @PathVariable String fileName, @AuthenticationPrincipal CustomUserDetails userDetails) {

          try {
              // 권한 확인: 해당 파일에 접근할 수 있는지 확인
              if (!exportSyncService.canAccessTempPdf(fileName, userDetails.getUserId())) {
                  return ResponseEntity.status(HttpStatus.FORBIDDEN)
                          .body(ApiResponse.error("해당 파일에 접근할 권한이 없습니다"));
              }

              // Redis에서 임시 URL 정보 조회
              String tempUrl = exportSyncService.getTempPdfUrl(fileName);

              if (tempUrl == null) {
                  return ResponseEntity.status(HttpStatus.NOT_FOUND)
                          .body(ApiResponse.error("파일을 찾을 수 없거나 만료되었습니다"));
              }

              // S3 URL로 리다이렉트
              return ResponseEntity.status(HttpStatus.FOUND).header("Location", tempUrl).build();

          } catch (Exception e) {
              log.error("임시 PDF 접근 실패", e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(ApiResponse.error("파일 접근에 실패했습니다"));
          }
      }

      @Override
      @GetMapping("/export/role")
      public ResponseEntity<ApiResponse<String>> getUserRole(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails) {
          String role = exportSyncService.getUserRole(contractChatId, userDetails.getUserId());
          return ResponseEntity.ok(ApiResponse.success(role));
      }

      @Override
      @GetMapping("/final_contract")
      public ResponseEntity<ApiResponse<MultipartFile>> finalContractPDF(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails) {
          return ResponseEntity.ok(
                  ApiResponse.success(
                          service.finalContractPDF(contractChatId, userDetails.getUserId())));
      }

      @PostMapping("/export/signature-status")
      public ResponseEntity<ApiResponse<ContractExportStatusDTO>> updateSignatureStatus(
              @PathVariable("contractChatId") Long contractChatId,
              @RequestBody SignatureSubmitDTO signatureData,
              @AuthenticationPrincipal CustomUserDetails userDetails) {

          log.info(
                  "HTTP API: 서명 상태 업데이트 요청 - contractChatId: {}, userRole: {}",
                  contractChatId,
                  signatureData.getUserRole());

          try {
              ContractExportStatusDTO updatedStatus =
                      exportSyncService.updateSignature(contractChatId, signatureData);
              return ResponseEntity.ok(ApiResponse.success(updatedStatus));
          } catch (Exception e) {
              log.error("서명 상태 업데이트 실패", e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(ApiResponse.error("서명 상태 업데이트에 실패했습니다"));
          }
      }

      @GetMapping("/export/status")
      public ResponseEntity<ApiResponse<ContractExportStatusDTO>> getExportStatus(
              @PathVariable("contractChatId") Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails) {

          try {
              ContractExportStatusDTO status = exportSyncService.getExportStatus(contractChatId);
              return ResponseEntity.ok(ApiResponse.success(status));
          } catch (Exception e) {
              log.error("내보내기 상태 조회 실패", e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(ApiResponse.error("상태 조회에 실패했습니다"));
          }
      }

      @Override
      @PostMapping("/signature/tax")
      public ResponseEntity<ApiResponse<Boolean>> saveSignature(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @RequestPart("dto") MultipartFile dtoFile,
              @RequestPart("imgFiles") List<MultipartFile> imgFiles)
              throws Exception {
          if (imgFiles == null || imgFiles.isEmpty()) {
              throw new IllegalArgumentException("서명 이미지가 비어 있습니다.");
          }

          // MultipartFile에서 문자열 추출
          String dtoText;
          try {
              dtoText = new String(dtoFile.getBytes(), "UTF-8");
          } catch (Exception e) {
              log.error("DTO 파일 읽기 실패: {}", e.getMessage());
              throw new IllegalArgumentException("DTO 파일을 읽을 수 없습니다.");
          }

          // 문자열 -> DTO (JSON 파싱)
          SaveSignatureDTO dto;
          try {
              dto = new ObjectMapper().readValue(dtoText, SaveSignatureDTO.class);
          } catch (Exception e) {
              log.error("DTO 파싱 실패: {}", e.getMessage());
              throw new IllegalArgumentException("잘못된 DTO 형식입니다.");
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

      // 서명된 PDF 다운로드 (역할에 따른 암호화)
      @PostMapping("/export/download-pdf")
      public ResponseEntity<byte[]> downloadSignedPdf(
              @PathVariable Long contractChatId,
              @AuthenticationPrincipal CustomUserDetails userDetails) {
          try {
              byte[] pdfData =
                      exportSyncService.getSignedPdfWithPassword(
                              contractChatId, userDetails.getUserId());

              HttpHeaders headers = new HttpHeaders();
              headers.setContentType(MediaType.APPLICATION_PDF);
              headers.setContentDisposition(
                      ContentDisposition.attachment()
                              .filename("contract_" + contractChatId + ".pdf")
                              .build());

              return ResponseEntity.ok().headers(headers).body(pdfData);
          } catch (Exception e) {
              log.error("Failed to download signed PDF", e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
          }
      }

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
