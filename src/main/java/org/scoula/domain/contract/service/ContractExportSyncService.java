package org.scoula.domain.contract.service;

import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.scoula.domain.chat.mapper.ContractChatMapper;
import org.scoula.domain.chat.vo.ContractChat;
import org.scoula.domain.contract.dto.*;
import org.scoula.global.file.service.S3ServiceInterface;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/** 계약서 내보내기 동기화 서비스 Redis를 사용하여 양측 상태를 관리하고 동기화 */
@Service
@RequiredArgsConstructor
@Log4j2
public class ContractExportSyncService {

      private final RedisTemplate<String, Object> redisTemplate;
      private final ContractService contractService;
      private final ContractChatMapper contractChatMapper;
      private final S3ServiceInterface s3Service;
      private final SimpMessagingTemplate messagingTemplate;
      private static final String EXPORT_STATUS_KEY = "contract:export:status:";
      private static final String EXPORT_PASSWORD_KEY = "contract:export:password:";
      private static final long EXPIRE_TIME = 2; // 2시간

      /** 계약서 내보내기 상태 조회 */
      public ContractExportStatusDTO getExportStatus(Long contractChatId) {
          try {
              String key = EXPORT_STATUS_KEY + contractChatId;
              ContractExportStatusDTO status =
                      (ContractExportStatusDTO) redisTemplate.opsForValue().get(key);

              if (status == null) {
                  log.info("Creating initial export status for contractChatId: {}", contractChatId);
                  // 초기 상태 생성
                  status = createInitialStatus(contractChatId);
                  saveExportStatus(contractChatId, status);
              }

              return status;
          } catch (Exception e) {
              log.error("Redis error, returning default status: ", e);
              // Redis 에러 시 기본 상태 반환
              return createInitialStatus(contractChatId);
          }
      }

      /** 계약서 내보내기 상태 저장 */
      public void saveExportStatus(Long contractChatId, ContractExportStatusDTO status) {
          String key = EXPORT_STATUS_KEY + contractChatId;
          status.setLastUpdated(System.currentTimeMillis());
          redisTemplate.opsForValue().set(key, status, EXPIRE_TIME, TimeUnit.HOURS);
      }

      /** 서명 업데이트 */
      public ContractExportStatusDTO updateSignature(
              Long contractChatId, SignatureSubmitDTO signatureData) {
          log.info(
                  "updateSignature called for contract {} with role {}",
                  contractChatId,
                  signatureData.getUserRole());
          ContractExportStatusDTO status = getExportStatus(contractChatId);
          log.info(
                  "Current status before update: owner={}, buyer={}",
                  status.isOwnerSignatureCompleted(),
                  status.isBuyerSignatureCompleted());

          if ("owner".equals(signatureData.getUserRole())) {
              // 임대인 서명 업데이트 - data URL 접두사 제거
              String sig1 = signatureData.getSignature1();
              String sig2 = signatureData.getSignature2();
              String sig3 = signatureData.getSignature3();

              // data:image/png;base64, 접두사 제거
              if (sig1 != null && sig1.startsWith("data:")) {
                  sig1 = sig1.substring(sig1.indexOf(",") + 1);
              }
              if (sig2 != null && sig2.startsWith("data:")) {
                  sig2 = sig2.substring(sig2.indexOf(",") + 1);
              }
              if (sig3 != null && sig3.startsWith("data:")) {
                  sig3 = sig3.substring(sig3.indexOf(",") + 1);
              }

              status.setOwnerSignatures(List.of(sig1, sig2, sig3));
              status.setOwnerSignatureCompleted(true);
              status.setOwnerHasTaxArrears(signatureData.isHasTaxArrears());
              status.setOwnerHasPriorFixedDate(signatureData.isHasPriorFixedDate());
              // 중재 동의는 무조건 true로 설정
              status.setOwnerMediationAgree(true);

          } else if ("buyer".equals(signatureData.getUserRole())) {
              // 임차인 서명 업데이트 - data URL 접두사 제거
              log.info("=== Buyer Signature Update ===");
              String buyerSig1 = signatureData.getSignature1();

              log.info(
                      "Buyer signature1 received: {}",
                      buyerSig1 != null ? "present (length: " + buyerSig1.length() + ")" : "null");

              // data:image/png;base64, 접두사 제거
              if (buyerSig1 != null && buyerSig1.startsWith("data:")) {
                  log.info("Removing data URL prefix from buyer signature");
                  buyerSig1 = buyerSig1.substring(buyerSig1.indexOf(",") + 1);
                  log.info("After removing prefix, length: {}", buyerSig1.length());
              }

              if (buyerSig1 != null && buyerSig1.length() > 0) {
                  log.info(
                          "Buyer signature1 preview after processing: {}",
                          buyerSig1.substring(0, Math.min(50, buyerSig1.length())));
              }

              status.setBuyerSignatures(List.of(buyerSig1));
              status.setBuyerSignatureCompleted(true);
              // 중재 동의는 무조건 true로 설정
              status.setBuyerMediationAgree(true);

              log.info(
                      "Buyer signatures after update: {}",
                      status.getBuyerSignatures() != null ? status.getBuyerSignatures().size() : 0);
              log.info("================================");
          }

          // 양측 서명 완료 확인
          log.info(
                  "Signature status after update - Owner: {}, Buyer: {}, Both: {}",
                  status.isOwnerSignatureCompleted(),
                  status.isBuyerSignatureCompleted(),
                  status.isBothSignaturesCompleted());

          // 양측 서명 완료 시 자동으로 최종 계약서 생성
          if (status.isBothSignaturesCompleted()) {
              log.info(
                      "Both signatures completed for contract {}. Auto-generating final contract with"
                              + " both signatures.",
                      contractChatId);
              status.setCurrentStep("generating");

              // 자동으로 최종 PDF 생성 시도
              try {
                  // 임대인과 임차인의 생년월일을 암호로 사용
                  String ownerPassword =
                          contractService.getUserBirthDate(
                                  contractChatId, status.getOwnerId(), "owner");
                  String buyerPassword =
                          contractService.getUserBirthDate(
                                  contractChatId, status.getBuyerId(), "buyer");

                  // Redis에 암호 저장
                  String ownerPasswordKey = EXPORT_PASSWORD_KEY + contractChatId + ":owner";
                  String buyerPasswordKey = EXPORT_PASSWORD_KEY + contractChatId + ":buyer";
                  redisTemplate
                          .opsForValue()
                          .set(ownerPasswordKey, ownerPassword, EXPIRE_TIME, TimeUnit.HOURS);
                  redisTemplate
                          .opsForValue()
                          .set(buyerPasswordKey, buyerPassword, EXPIRE_TIME, TimeUnit.HOURS);

                  status.setOwnerPasswordSet(true);
                  status.setBuyerPasswordSet(true);

                  // 실제 서명이 포함된 최종 PDF 생성
                  try {
                      log.info("Starting PDF generation for contract {}", contractChatId);
                      String finalPdfUrl = generateSignedPdf(contractChatId, status);

                      if (finalPdfUrl != null && !finalPdfUrl.isEmpty()) {
                          status.setCurrentStep("complete");
                          status.setCompleted(true);
                          status.setFinalPdfUrl(finalPdfUrl);

                          log.info(
                                  "Final signed PDF generated for contract {}: {}",
                                  contractChatId,
                                  finalPdfUrl);
                      } else {
                          log.error("PDF URL is null or empty for contract {}", contractChatId);
                          status.setCurrentStep("error");
                          status.setFinalPdfUrl(null);
                      }
                  } catch (Exception pdfError) {
                      log.error(
                              "Failed to generate signed PDF for contract {}: {}",
                              contractChatId,
                              pdfError.getMessage(),
                              pdfError);
                      status.setCurrentStep("error");
                      status.setFinalPdfUrl(null);
                  }
              } catch (Exception e) {
                  log.error("Failed to auto-generate final PDF for contract {}", contractChatId, e);
                  status.setCurrentStep("password"); // 실패 시 암호 단계로
              }
          } else {
              // 한쪽만 서명한 경우 - 대기 상태 설정
              log.info(
                      "Waiting for other party's signature for contract {}. Owner signed: {}, Buyer"
                              + " signed: {}",
                      contractChatId,
                      status.isOwnerSignatureCompleted(),
                      status.isBuyerSignatureCompleted());
              status.setCurrentStep("waiting");
          }

          saveExportStatus(contractChatId, status);

          // WebSocket으로 상태 브로드캐스트 (상대방에게 알림)
          broadcastStatusUpdate(contractChatId, status);

          return status;
      }

      /** 암호 업데이트 */
      public ContractExportStatusDTO updatePassword(
              Long contractChatId, PasswordSubmitDTO passwordData) {
          ContractExportStatusDTO status = getExportStatus(contractChatId);

          // Redis에 암호 저장 (보안을 위해 별도 키로 저장)
          String passwordKey =
                  EXPORT_PASSWORD_KEY + contractChatId + ":" + passwordData.getUserRole();
          redisTemplate
                  .opsForValue()
                  .set(passwordKey, passwordData.getPassword(), EXPIRE_TIME, TimeUnit.HOURS);

          if ("owner".equals(passwordData.getUserRole())) {
              status.setOwnerPasswordSet(true);
          } else if ("buyer".equals(passwordData.getUserRole())) {
              status.setBuyerPasswordSet(true);
          }

          // 양측 암호 설정 완료 시 최종 PDF 생성 준비
          if (status.isBothPasswordsSet()) {
              status.setCurrentStep("generating");
              log.info(
                      "Both passwords set for contract {}. Ready to generate final PDF.",
                      contractChatId);
          }

          saveExportStatus(contractChatId, status);
          return status;
      }

      /** 최종 PDF 생성 */
      public String generateFinalPdf(Long contractChatId) throws Exception {
          ContractExportStatusDTO status = getExportStatus(contractChatId);

          if (!status.isReadyForCompletion()) {
              throw new IllegalStateException(
                      "Not ready for PDF generation. Missing signatures or passwords.");
          }

          // 양측 암호 가져오기
          String ownerPassword =
                  (String)
                          redisTemplate
                                  .opsForValue()
                                  .get(EXPORT_PASSWORD_KEY + contractChatId + ":owner");
          String buyerPassword =
                  (String)
                          redisTemplate
                                  .opsForValue()
                                  .get(EXPORT_PASSWORD_KEY + contractChatId + ":buyer");

          // 암호 결합 (예: 두 암호를 연결하거나 XOR 등의 방식 사용)
          String combinedPassword = combinePasswords(ownerPassword, buyerPassword);

          // 최종 PDF 생성 (기존 서비스 활용)
          ContractPasswordDTO passwordDTO = new ContractPasswordDTO();
          passwordDTO.setContractPassword(combinedPassword);
          passwordDTO.setMediationAgree(
                  status.isOwnerMediationAgree() && status.isBuyerMediationAgree());

          // PDF 생성 및 S3 업로드
          byte[] finalPdf =
                  contractService.saveFinalContract(contractChatId, status.getOwnerId(), passwordDTO);

          // S3 URL 반환 (실제 구현 필요)
          String pdfUrl = uploadToS3(contractChatId, finalPdf);

          // 상태 업데이트
          status.setCompleted(true);
          status.setFinalPdfUrl(pdfUrl);
          status.setCurrentStep("complete");
          saveExportStatus(contractChatId, status);

          // 임시 데이터 정리
          cleanupTempData(contractChatId);

          return pdfUrl;
      }

      /** 초기 상태 생성 */
      private ContractExportStatusDTO createInitialStatus(Long contractChatId) {
          // DB에서 계약 정보 조회하여 초기 상태 생성
          log.debug("ContractChatMapper is null? {}", contractChatMapper == null);

          if (contractChatMapper == null) {
              log.error("ContractChatMapper is not injected!");
              throw new IllegalStateException("ContractChatMapper is not available");
          }

          ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);

          if (contractChat == null) {
              log.error("Contract chat not found for id: {}", contractChatId);
              throw new IllegalArgumentException("Contract chat not found");
          }

          return ContractExportStatusDTO.builder()
                  .contractChatId(contractChatId)
                  .ownerId(contractChat.getOwnerId())
                  .buyerId(contractChat.getBuyerId())
                  .currentStep("preview")
                  .ownerSignatureCompleted(false)
                  .buyerSignatureCompleted(false)
                  .ownerPasswordSet(false)
                  .buyerPasswordSet(false)
                  .isCompleted(false)
                  .lastUpdated(System.currentTimeMillis())
                  .build();
      }

      /** 암호 결합 로직 */
      private String combinePasswords(String password1, String password2) {
          // 간단한 결합 방식: 두 암호를 연결
          // 실제로는 더 복잡한 암호화 방식 사용 가능
          return password1 + "_" + password2;
      }

      /** S3 업로드 */
      private String uploadToS3(Long contractChatId, byte[] pdfData) {
          String fileName = "contract_" + contractChatId + "_" + System.currentTimeMillis() + ".pdf";

          // byte array를 MultipartFile로 변환
          MultipartFile multipartFile =
                  new MultipartFile() {
                      @Override
                      public String getName() {
                          return fileName;
                      }

                      @Override
                      public String getOriginalFilename() {
                          return fileName;
                      }

                      @Override
                      public String getContentType() {
                          return "application/pdf";
                      }

                      @Override
                      public boolean isEmpty() {
                          return pdfData == null || pdfData.length == 0;
                      }

                      @Override
                      public long getSize() {
                          return pdfData.length;
                      }

                      @Override
                      public byte[] getBytes() {
                          return pdfData;
                      }

                      @Override
                      public InputStream getInputStream() {
                          return new ByteArrayInputStream(pdfData);
                      }

                      @Override
                      public void transferTo(File dest) throws IOException {
                          Files.write(dest.toPath(), pdfData);
                      }
                  };

          String s3Key = s3Service.uploadFile(multipartFile, fileName);
          return s3Service.getFileUrl(s3Key);
      }

      /** 임시 데이터 정리 */
      private void cleanupTempData(Long contractChatId) {
          // Redis에서 암호 데이터 삭제
          redisTemplate.delete(EXPORT_PASSWORD_KEY + contractChatId + ":owner");
          redisTemplate.delete(EXPORT_PASSWORD_KEY + contractChatId + ":buyer");
          log.info("Cleaned up temporary data for contract {}", contractChatId);
      }

      /** 사용자 역할 확인 */
      public String getUserRole(Long contractChatId, Long userId) {
          try {
              // ContractExportStatusDTO에서 역할 확인
              ContractExportStatusDTO status = getExportStatus(contractChatId);

              if (status != null) {
                  if (status.getOwnerId() != null && status.getOwnerId().equals(userId)) {
                      return "owner";
                  } else if (status.getBuyerId() != null && status.getBuyerId().equals(userId)) {
                      return "buyer";
                  }
              }

              // 상태에 정보가 없으면 DB에서 직접 조회
              log.info(
                      "Checking user role directly from DB for contractChatId: {}, userId: {}",
                      contractChatId,
                      userId);
              ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
              if (contractChat != null) {
                  if (contractChat.getOwnerId() != null && contractChat.getOwnerId().equals(userId)) {
                      return "owner";
                  } else if (contractChat.getBuyerId() != null
                          && contractChat.getBuyerId().equals(userId)) {
                      return "buyer";
                  }
              }

              log.warn(
                      "Could not determine user role for contractChatId: {}, userId: {}",
                      contractChatId,
                      userId);
              return "owner"; // 기본값
          } catch (Exception e) {
              log.error("Error determining user role: ", e);
              return "owner"; // 에러 시 기본값
          }
      }

      /** 임시 PDF 업로드 및 URL 생성 (임차인/임대인만 접근 가능) */
      public String uploadTempPdf(byte[] pdfBytes, String fileName) {
          try {
              // S3에 임시 파일 업로드 (temp 폴더에 저장)
              String tempFileName = "temp/" + fileName;

              // byte array를 MultipartFile로 변환
              MultipartFile multipartFile =
                      new MultipartFile() {
                          @Override
                          public String getName() {
                              return tempFileName;
                          }

                          @Override
                          public String getOriginalFilename() {
                              return tempFileName;
                          }

                          @Override
                          public String getContentType() {
                              return "application/pdf";
                          }

                          @Override
                          public boolean isEmpty() {
                              return pdfBytes == null || pdfBytes.length == 0;
                          }

                          @Override
                          public long getSize() {
                              return pdfBytes.length;
                          }

                          @Override
                          public byte[] getBytes() {
                              return pdfBytes;
                          }

                          @Override
                          public InputStream getInputStream() {
                              return new ByteArrayInputStream(pdfBytes);
                          }

                          @Override
                          public void transferTo(File dest) throws IOException {
                              Files.write(dest.toPath(), pdfBytes);
                          }
                      };

              String s3Key = s3Service.uploadFile(multipartFile, tempFileName);

              // S3 Key를 전체 URL로 변환
              String tempUrl = s3Service.getFileUrl(s3Key);

              // Redis에 임시 URL 정보 저장 (1시간 후 만료)
              String tempKey = "temp:pdf:" + fileName;
              redisTemplate.opsForValue().set(tempKey, tempUrl, 1, TimeUnit.HOURS);

              log.info("Temporary PDF uploaded with S3 key: {}, URL: {}", s3Key, tempUrl);
              return tempUrl;

          } catch (Exception e) {
              log.error("Failed to upload temporary PDF", e);
              throw new RuntimeException("임시 PDF 업로드에 실패했습니다", e);
          }
      }

      /** 임시 PDF 접근 권한 확인 */
      public boolean canAccessTempPdf(String fileName, Long userId) {
          try {
              // 파일명에서 계약서 ID 추출 (contract_123_timestamp.pdf 형식)
              String[] parts = fileName.split("_");
              if (parts.length < 2) {
                  return false;
              }

              Long contractChatId = Long.parseLong(parts[1]);

              // 해당 계약서의 참여자인지 확인
              String userRole = getUserRole(contractChatId, userId);
              return "owner".equals(userRole) || "buyer".equals(userRole);

          } catch (Exception e) {
              log.error("Error checking PDF access permission", e);
              return false;
          }
      }

      /** 임시 PDF URL 조회 */
      public String getTempPdfUrl(String fileName) {
          String tempKey = "temp:pdf:" + fileName;
          return (String) redisTemplate.opsForValue().get(tempKey);
      }

      /** 서명이 포함된 최종 PDF 생성 */
      private String generateSignedPdf(Long contractChatId, ContractExportStatusDTO status)
              throws Exception {
          log.info("Generating signed PDF for contract {} with signatures", contractChatId);
          log.info("Status - OwnerId: {}, BuyerId: {}", status.getOwnerId(), status.getBuyerId());
          log.info(
                  "Owner signatures count: {}",
                  status.getOwnerSignatures() != null ? status.getOwnerSignatures().size() : 0);
          log.info(
                  "Buyer signatures count: {}",
                  status.getBuyerSignatures() != null ? status.getBuyerSignatures().size() : 0);

          // 무조건 서명과 동의 여부가 포함된 PDF 생성
          log.info("Creating PDF with signatures and agreement status");
          byte[] signedPdf = createSignedPdfWithSignatures(contractChatId, status);

          if (signedPdf == null || signedPdf.length == 0) {
              log.error("Failed to create signed PDF - signedPdf is null or empty");
              throw new RuntimeException("PDF 생성 실패 - 서명이 포함된 PDF를 생성할 수 없습니다");
          }

          log.info("Successfully created signed PDF with size: {} bytes", signedPdf.length);

          // 임대인과 임차인의 생년월일 가져오기 (주민번호 앞자리 사용)
          String ownerBirthDate =
                  contractService.getUserBirthDate(contractChatId, status.getOwnerId(), "owner");
          String buyerBirthDate =
                  contractService.getUserBirthDate(contractChatId, status.getBuyerId(), "buyer");

          // 두 생년월일을 조합한 암호화 키 생성
          String combinedKey = ownerBirthDate + "_" + buyerBirthDate;

          // PDF 암호화
          byte[] encryptedPdf = encryptPdfWithCombinedKey(signedPdf, combinedKey);

          // 암호화된 PDF의 해시값 계산
          String pdfHash = calculateHash(encryptedPdf);

          // S3에 암호화된 최종 PDF 업로드 (DB 저장용)
          String encryptedFileName = String.format("encrypted/final_contract_%d.pdf", contractChatId);

          // byte array를 MultipartFile로 변환
          MultipartFile encryptedFile =
                  new MultipartFile() {
                      @Override
                      public String getName() {
                          return encryptedFileName;
                      }

                      @Override
                      public String getOriginalFilename() {
                          return encryptedFileName;
                      }

                      @Override
                      public String getContentType() {
                          return "application/pdf";
                      }

                      @Override
                      public boolean isEmpty() {
                          return encryptedPdf == null || encryptedPdf.length == 0;
                      }

                      @Override
                      public long getSize() {
                          return encryptedPdf.length;
                      }

                      @Override
                      public byte[] getBytes() {
                          return encryptedPdf;
                      }

                      @Override
                      public InputStream getInputStream() {
                          return new ByteArrayInputStream(encryptedPdf);
                      }

                      @Override
                      public void transferTo(File dest) throws IOException {
                          Files.write(dest.toPath(), encryptedPdf);
                      }
                  };

          String encryptedS3Key = s3Service.uploadFile(encryptedFile, encryptedFileName);
          String encryptedS3Url = s3Service.getFileUrl(encryptedS3Key);

          // final_contract 테이블에 암호화된 PDF 정보 저장
          contractService.saveFinalContractToDatabase(contractChatId, encryptedS3Url, pdfHash);
          log.info(
                  "Saved encrypted PDF to database: contractChatId={}, url={}, hash={}",
                  contractChatId,
                  encryptedS3Url,
                  pdfHash);

          // 사용자에게 보여줄 서명된 PDF (암호화 없이) S3에 업로드 - 영구 보관용
          String finalFileName = String.format("final/contract_%d.pdf", contractChatId);

          // byte array를 MultipartFile로 변환
          MultipartFile signedFile =
                  new MultipartFile() {
                      @Override
                      public String getName() {
                          return finalFileName;
                      }

                      @Override
                      public String getOriginalFilename() {
                          return finalFileName;
                      }

                      @Override
                      public String getContentType() {
                          return "application/pdf";
                      }

                      @Override
                      public boolean isEmpty() {
                          return signedPdf == null || signedPdf.length == 0;
                      }

                      @Override
                      public long getSize() {
                          return signedPdf.length;
                      }

                      @Override
                      public byte[] getBytes() {
                          return signedPdf;
                      }

                      @Override
                      public InputStream getInputStream() {
                          return new ByteArrayInputStream(signedPdf);
                      }

                      @Override
                      public void transferTo(File dest) throws IOException {
                          Files.write(dest.toPath(), signedPdf);
                      }
                  };

          // 암호화되지 않은 서명 PDF를 S3에 업로드하고 전체 URL 받기
          String signedPdfKey = s3Service.uploadFile(signedFile, finalFileName);
          String signedPdfUrl = s3Service.getFileUrl(signedPdfKey);

          log.info(
                  "Final contract saved - contractChatId: {}, Encrypted URL: {}, Signed PDF URL: {}",
                  contractChatId,
                  encryptedS3Url,
                  signedPdfUrl);

          // 서명 완료 알림 (WebSocket) - 서명된 PDF URL 전송
          broadcastContractCompletion(contractChatId, signedPdfUrl);

          return signedPdfUrl;
      }

      /** 상태 업데이트 브로드캐스트 */
      private void broadcastStatusUpdate(Long contractChatId, ContractExportStatusDTO status) {
          try {
              messagingTemplate.convertAndSend(
                      "/topic/contract/" + contractChatId + "/export/status", status);
              log.info("Status update broadcasted for contract {}", contractChatId);
          } catch (Exception e) {
              log.error("Failed to broadcast status update", e);
          }
      }

      /** 계약 완료 알림 브로드캐스트 */
      private void broadcastContractCompletion(Long contractChatId, String finalPdfUrl) {
          try {
              ContractCompletionMessage message =
                      ContractCompletionMessage.builder()
                              .contractChatId(contractChatId)
                              .finalPdfUrl(finalPdfUrl)
                              .completedAt(System.currentTimeMillis())
                              .message("양측 서명이 완료되어 최종 계약서가 생성되었습니다.")
                              .build();

              messagingTemplate.convertAndSend(
                      "/topic/contract/" + contractChatId + "/completion", message);

              log.info("Contract completion broadcasted for contract {}", contractChatId);
          } catch (Exception e) {
              log.error("Failed to broadcast contract completion", e);
          }
      }

      // 계약 완료 메시지 클래스
      @lombok.Builder
      @lombok.Data
      private static class ContractCompletionMessage {
          private Long contractChatId;
          private String finalPdfUrl;
          private long completedAt;
          private String message;
      }

      /** 서명된 PDF를 사용자 역할에 맞는 암호로 보호하여 반환 */
      public byte[] getSignedPdfWithPassword(Long contractChatId, Long userId) throws Exception {
          ContractExportStatusDTO status = getExportStatus(contractChatId);

          // 완료된 PDF URL 확인
          if (status == null || !status.isCompleted() || status.getFinalPdfUrl() == null) {
              throw new IllegalStateException("최종 계약서가 아직 생성되지 않았습니다.");
          }

          // 사용자 역할 확인
          String userRole = getUserRole(contractChatId, userId);

          // 역할에 따른 생년월일 가져오기 (YYMMDD 형식)
          String birthDate = contractService.getUserBirthDate(contractChatId, userId, userRole);

          // S3에서 최종 PDF 다운로드
          String s3Key = extractS3KeyFromUrl(status.getFinalPdfUrl());
          InputStream inputStream = s3Service.downloadFile(s3Key);
          ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
          byte[] buffer = new byte[1024];
          int bytesRead;
          while ((bytesRead = inputStream.read(buffer)) != -1) {
              outputStream.write(buffer, 0, bytesRead);
          }
          byte[] pdfData = outputStream.toByteArray();

          // 생년월일로 암호화된 PDF 생성
          return encryptPdfWithPassword(pdfData, birthDate);
      }

      /** S3 URL에서 키 추출 */
      private String extractS3KeyFromUrl(String url) {
          // URL에서 S3 키 추출 로직
          // 예: https://bucket.s3.amazonaws.com/path/to/file.pdf -> path/to/file.pdf
          if (url.contains(".amazonaws.com/")) {
              return url.substring(url.lastIndexOf(".com/") + 5);
          }
          return url;
      }

      /** PDF를 암호로 보호 */
      private byte[] encryptPdfWithPassword(byte[] pdfData, String password) throws Exception {
          // iText7를 사용한 PDF 암호화
          ByteArrayOutputStream baos = new ByteArrayOutputStream();

          try {
              com.itextpdf.kernel.pdf.PdfReader reader =
                      new com.itextpdf.kernel.pdf.PdfReader(
                              new java.io.ByteArrayInputStream(pdfData));
              com.itextpdf.kernel.pdf.PdfWriter writer =
                      new com.itextpdf.kernel.pdf.PdfWriter(
                              baos,
                              new com.itextpdf.kernel.pdf.WriterProperties()
                                      .setStandardEncryption(
                                              password.getBytes(),
                                              password.getBytes(),
                                              com.itextpdf.kernel.pdf.EncryptionConstants
                                                              .ALLOW_PRINTING
                                                      | com.itextpdf.kernel.pdf.EncryptionConstants
                                                              .ALLOW_COPY,
                                              com.itextpdf.kernel.pdf.EncryptionConstants
                                                      .ENCRYPTION_AES_128));

              com.itextpdf.kernel.pdf.PdfDocument pdfDoc =
                      new com.itextpdf.kernel.pdf.PdfDocument(reader, writer);
              pdfDoc.close();

          } catch (Exception e) {
              log.error("Failed to encrypt PDF", e);
              throw e;
          }

          return baos.toByteArray();
      }

      /** 조합된 키로 PDF 암호화 */
      private byte[] encryptPdfWithCombinedKey(byte[] pdfData, String combinedKey) throws Exception {
          ByteArrayOutputStream baos = new ByteArrayOutputStream();

          try {
              com.itextpdf.kernel.pdf.PdfReader reader =
                      new com.itextpdf.kernel.pdf.PdfReader(
                              new java.io.ByteArrayInputStream(pdfData));
              com.itextpdf.kernel.pdf.PdfWriter writer =
                      new com.itextpdf.kernel.pdf.PdfWriter(
                              baos,
                              new com.itextpdf.kernel.pdf.WriterProperties()
                                      .setStandardEncryption(
                                              combinedKey.getBytes(),
                                              combinedKey.getBytes(),
                                              com.itextpdf.kernel.pdf.EncryptionConstants
                                                              .ALLOW_PRINTING
                                                      | com.itextpdf.kernel.pdf.EncryptionConstants
                                                              .ALLOW_COPY,
                                              com.itextpdf.kernel.pdf.EncryptionConstants
                                                      .ENCRYPTION_AES_256));

              com.itextpdf.kernel.pdf.PdfDocument pdfDoc =
                      new com.itextpdf.kernel.pdf.PdfDocument(reader, writer);
              pdfDoc.close();

          } catch (Exception e) {
              log.error("Failed to encrypt PDF with combined key", e);
              throw e;
          }

          return baos.toByteArray();
      }

      /** PDF 해시값 계산 */
      private String calculateHash(byte[] data) {
          try {
              MessageDigest digest = MessageDigest.getInstance("SHA-256");
              byte[] hash = digest.digest(data);
              return Base64.getEncoder().encodeToString(hash);
          } catch (Exception e) {
              log.error("Failed to calculate hash", e);
              throw new RuntimeException("Hash calculation failed", e);
          }
      }

      /** 서명이 포함된 PDF 직접 생성 - AI 서버 사용 */
      private byte[] createSignedPdfWithSignatures(
              Long contractChatId, ContractExportStatusDTO status) {
          try {
              log.info(
                      "Creating signed PDF with signatures using AI server for contract {}",
                      contractChatId);

              // 임차인 서명 확인 로그 추가
              if (status.getBuyerSignatures() != null && !status.getBuyerSignatures().isEmpty()) {
                  log.info(
                          "Buyer signatures present: {} signatures",
                          status.getBuyerSignatures().size());
                  for (int i = 0; i < status.getBuyerSignatures().size(); i++) {
                      String sig = status.getBuyerSignatures().get(i);
                      if (sig != null) {
                          log.info("Buyer signature {} length: {}", i + 1, sig.length());
                      }
                  }
              } else {
                  log.warn("No buyer signatures found in status!");
              }

              // AI 서버를 사용하여 전체 데이터 + 동의 여부 + 서명 이미지로 PDF 생성
              byte[] signedPdf = generatePdfWithAiServer(contractChatId, status);

              if (signedPdf == null || signedPdf.length == 0) {
                  log.error("Failed to generate signed PDF from AI server, trying fallback");
                  // AI 서버 실패 시 fallback PDF 생성
                  signedPdf = createFallbackPdf(contractChatId, status);
              }

              if (signedPdf == null || signedPdf.length == 0) {
                  log.error("Failed to generate any PDF including fallback");
                  throw new RuntimeException("AI 서버 및 fallback PDF 생성 모두 실패");
              }

              log.info("Signed PDF generated successfully with size: {} bytes", signedPdf.length);
              return signedPdf;

          } catch (Exception e) {
              log.error("Failed to create signed PDF with signatures: ", e);
              log.error("Error type: {}", e.getClass().getName());
              log.error("Error message: {}", e.getMessage());
              if (e.getCause() != null) {
                  log.error("Cause: {}", e.getCause().getMessage());
              }
              // 에러 발생 시 null 반환하여 실패를 명확히 함
              throw new RuntimeException("PDF 생성 실패: " + e.getMessage(), e);
          }
      }

      /** AI 서버를 사용하여 전체 데이터 + 동의 여부 + 서명 이미지로 PDF 생성 */
      private byte[] generatePdfWithAiServer(Long contractChatId, ContractExportStatusDTO status) {
          try {
              log.info(
                      "Generating PDF with AI server for contract {} with full data + signatures",
                      contractChatId);

              // 서명 데이터 상세 로깅
              log.info("=== Signature Data Analysis ===");
              log.info(
                      "Owner signatures count: {}",
                      status.getOwnerSignatures() != null ? status.getOwnerSignatures().size() : 0);
              if (status.getOwnerSignatures() != null) {
                  for (int i = 0; i < status.getOwnerSignatures().size(); i++) {
                      String sig = status.getOwnerSignatures().get(i);
                      log.info(
                              "Owner signature {}: {} (length: {})",
                              i + 1,
                              sig != null ? "present" : "null",
                              sig != null ? sig.length() : 0);
                  }
              }

              log.info(
                      "Buyer signatures count: {}",
                      status.getBuyerSignatures() != null ? status.getBuyerSignatures().size() : 0);
              if (status.getBuyerSignatures() != null) {
                  for (int i = 0; i < status.getBuyerSignatures().size(); i++) {
                      String sig = status.getBuyerSignatures().get(i);
                      log.info(
                              "Buyer signature {}: {} (length: {})",
                              i + 1,
                              sig != null ? "present" : "null",
                              sig != null ? sig.length() : 0);
                      if (sig != null && sig.length() > 0) {
                          log.info(
                                  "Buyer signature {} preview: {}",
                                  i + 1,
                                  sig.substring(0, Math.min(50, sig.length())));
                      }
                  }
              } else {
                  log.error("❌ Buyer signatures list is NULL!");
              }
              log.info("===============================");

              // ContractService의 generateContractWithSignatures 메서드 사용
              // 사용자 ID는 임대인 ID를 사용 (권한 확인용)
              byte[] pdfBytes =
                      contractService.generateContractWithSignatures(
                              contractChatId,
                              status.getOwnerId(), // userId for validation
                              status.getOwnerSignatures(),
                              status.getBuyerSignatures(),
                              status.isOwnerHasTaxArrears(),
                              status.isOwnerHasPriorFixedDate(),
                              status.isOwnerMediationAgree(),
                              status.isBuyerMediationAgree());

              if (pdfBytes != null && pdfBytes.length > 0) {
                  log.info("AI server PDF generation successful, size: {} bytes", pdfBytes.length);
                  return pdfBytes;
              } else {
                  log.error("AI server returned empty or null PDF data");
                  return null;
              }

          } catch (Exception e) {
              log.error("Failed to generate PDF with AI server: ", e);
              return null;
          }
      }

      /** Fallback PDF 생성 (AI 서버 실패 시) */
      private byte[] createFallbackPdf(Long contractChatId, ContractExportStatusDTO status) {
          try {
              log.info("Creating fallback PDF for contract {}", contractChatId);

              // 기존 계약서 PDF 가져오기 시도
              try {
                  // MongoDB에서 기존 계약서 데이터 조회
                  byte[] existingPdf = contractService.getExistingContractPdf(contractChatId);
                  if (existingPdf != null && existingPdf.length > 0) {
                      log.info("Using existing contract PDF as fallback");
                      return existingPdf;
                  }
              } catch (Exception e) {
                  log.warn("Could not retrieve existing PDF: ", e);
              }

              // AI 서버를 사용한 fallback PDF 생성 시도
              log.info("Attempting to generate fallback PDF using AI server");
              try {
                  // AI 서버에 서명 없는 버전으로 요청 (기본 계약서 생성)
                  byte[] fallbackPdf =
                          contractService.startContractExport(contractChatId, status.getOwnerId());
                  if (fallbackPdf != null && fallbackPdf.length > 0) {
                      log.info(
                              "Fallback PDF generated using AI server, size: {} bytes",
                              fallbackPdf.length);
                      return fallbackPdf;
                  }
              } catch (Exception aiException) {
                  log.warn("AI server fallback also failed: ", aiException);
              }

              // 최후의 수단으로 null 반환 (완전한 실패)
              log.error("All PDF generation methods failed for contract {}", contractChatId);
              return null;

          } catch (Exception e) {
              log.error("Failed to create fallback PDF", e);
              return null;
          }
      }
}
