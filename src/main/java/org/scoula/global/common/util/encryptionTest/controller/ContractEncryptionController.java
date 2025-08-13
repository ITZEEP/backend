package org.scoula.global.common.util.encryptionTest.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.scoula.global.common.util.EncryptionUtil;
import org.scoula.global.common.util.EncryptionUtil.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/** Redis 기반 계약서 키 수집 및 자동 암호화 컨트롤러 */
@RestController
@RequestMapping("/api/contract/encryption")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Log4j2
public class ContractEncryptionController {

      private final EncryptionUtil encryptionUtil;
      private final ObjectMapper objectMapper = new ObjectMapper();

      /** 1단계: 계약서 업로드 및 첫 번째 키 등록 (암호화된 데이터 포함) */
      @PostMapping("/step1/upload")
      public ResponseEntity<?> step1UploadContract(
              @RequestParam("file") MultipartFile pdfFile,
              @RequestParam("contractChatId") String contractChatId,
              @RequestParam("keyType") String keyType,
              @RequestParam("password") String password) {

          try {
              log.info(
                      "Step 1: Uploading contract - contractChatId: {}, keyType: {}",
                      contractChatId,
                      keyType);

              // 파일 업로드 및 첫 번째 키 등록
              byte[] pdfData = pdfFile.getBytes();
              ContractKeyStatus status =
                      encryptionUtil.uploadContract(contractChatId, pdfData, keyType, password);

              Map<String, Object> response = new HashMap<>();
              response.put("success", true);
              response.put("step", 1);
              response.put("contractChatId", status.getFileId());
              response.put("status", status.getStatus());
              response.put("message", status.getMessage());
              response.put("nextStep", "Upload second key using /step2/add-key endpoint");

              // 암호화된 PDF 데이터를 Base64로 인코딩하여 반환
              response.put("encryptedPdfData", java.util.Base64.getEncoder().encodeToString(pdfData));
              response.put("originalFileName", pdfFile.getOriginalFilename());

              if ("WAITING_TENANT_KEY".equals(status.getStatus())) {
                  response.put("waitingFor", "tenant");
                  response.put("hasOwnerKey", true);
                  response.put("hasTenantKey", false);
              } else {
                  response.put("waitingFor", "owner");
                  response.put("hasOwnerKey", false);
                  response.put("hasTenantKey", true);
              }

              return ResponseEntity.ok(response);

          } catch (Exception e) {
              log.error("Step 1 failed: {}", e.getMessage(), e);
              Map<String, Object> error = new HashMap<>();
              error.put("success", false);
              error.put("step", 1);
              error.put("error", e.getMessage());
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
          }
      }

      /** 2단계: 두 번째 키 추가 (자동 암호화 트리거 및 파일 다운로드) */
      @PostMapping("/step2/add-key")
      public ResponseEntity<?> step2AddSecondKey(
              @RequestParam("contractChatId") String contractChatId,
              @RequestParam("keyType") String keyType,
              @RequestParam("password") String password) {

          try {
              log.info(
                      "Step 2: Adding second key - contractChatId: {}, keyType: {}",
                      contractChatId,
                      keyType);

              // 두 번째 키 추가
              ContractEncryptionResult result =
                      encryptionUtil.addSecondKey(contractChatId, keyType, password);

              if ("ENCRYPTION_COMPLETED".equals(result.getStatus())) {
                  log.info(
                          "Step 2 Complete: Encryption successful for contractChatId: {}",
                          contractChatId);

                  // 암호화된 파일 데이터 준비
                  EncryptedPDF encryptedPDF = result.getEncryptedPDF();
                  Map<String, Object> metadata = new HashMap<>();
                  metadata.put("contractChatId", contractChatId);
                  metadata.put("originalHash", encryptedPDF.originalHash);
                  metadata.put("iv", encryptedPDF.iv);
                  metadata.put("encryptedShares", encryptedPDF.encryptedShares);
                  metadata.put("threshold", encryptedPDF.threshold);
                  metadata.put("totalShares", encryptedPDF.totalShares);
                  metadata.put("encryptionTimestamp", encryptedPDF.encryptionTimestamp);

                  Map<String, Object> encryptedFileData = new HashMap<>();
                  encryptedFileData.put(
                          "encryptedData",
                          java.util.Base64.getEncoder().encodeToString(encryptedPDF.encryptedData));
                  encryptedFileData.put("metadata", metadata);

                  // JSON으로 변환
                  byte[] jsonData = objectMapper.writeValueAsBytes(encryptedFileData);

                  // 파일명 생성
                  String fileName = contractChatId + "_encrypted.json";

                  // 바로 파일로 다운로드
                  HttpHeaders headers = new HttpHeaders();
                  headers.setContentType(MediaType.APPLICATION_JSON);
                  headers.setContentDispositionFormData("attachment", fileName);
                  headers.add("X-Contract-Chat-Id", contractChatId);
                  headers.add("X-Status", "ENCRYPTION_COMPLETED");
                  headers.add("X-Message", "Encryption completed successfully. File downloaded.");

                  log.info("Returning encrypted file for download: {}", contractChatId);

                  return ResponseEntity.ok().headers(headers).body(jsonData);

              } else {
                  // 아직 키가 하나 더 필요한 경우
                  Map<String, Object> response = new HashMap<>();
                  response.put("success", true);
                  response.put("step", 2);
                  response.put("contractChatId", result.getFileId());
                  response.put("status", result.getStatus());
                  response.put("message", result.getMessage());
                  response.put("note", "Still waiting for one more key");

                  return ResponseEntity.ok(response);
              }

          } catch (IllegalArgumentException e) {
              log.error("Step 2 validation error: {}", e.getMessage());
              Map<String, Object> error = new HashMap<>();
              error.put("success", false);
              error.put("step", 2);
              error.put("error", e.getMessage());
              return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
          } catch (Exception e) {
              log.error("Step 2 failed: {}", e.getMessage(), e);
              Map<String, Object> error = new HashMap<>();
              error.put("success", false);
              error.put("step", 2);
              error.put("error", e.getMessage());
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
          }
      }

      /** 계약서 복호화 - 암호화된 JSON 파일을 받아서 PDF로 복호화 */
      @PostMapping("/decrypt")
      public void decryptContract(
              @RequestParam("file") MultipartFile encryptedFile,
              @RequestParam(value = "ownerPassword", required = false) String ownerPassword,
              @RequestParam(value = "tenantPassword", required = false) String tenantPassword,
              HttpServletResponse response) {

          try {
              log.info("Decrypting contract file: {}", encryptedFile.getOriginalFilename());

              // JSON 파일 파싱
              byte[] jsonData = encryptedFile.getBytes();
              Map<String, Object> encryptedFileData = objectMapper.readValue(jsonData, Map.class);

              // EncryptedPDF 객체 재구성
              EncryptedPDF encryptedPDF = new EncryptedPDF();

              // 암호화된 데이터 디코딩
              String encodedData = (String) encryptedFileData.get("encryptedData");
              encryptedPDF.encryptedData = java.util.Base64.getDecoder().decode(encodedData);

              // 메타데이터 복원
              Map<String, Object> metadata = (Map<String, Object>) encryptedFileData.get("metadata");
              encryptedPDF.originalHash = (String) metadata.get("originalHash");
              encryptedPDF.iv = (String) metadata.get("iv");
              encryptedPDF.threshold = (Integer) metadata.get("threshold");
              encryptedPDF.totalShares = (Integer) metadata.get("totalShares");

              // 암호화된 shares 복원
              Map<String, Map<String, String>> sharesData =
                      (Map<String, Map<String, String>>) metadata.get("encryptedShares");
              encryptedPDF.encryptedShares = new HashMap<>();

              for (Map.Entry<String, Map<String, String>> entry : sharesData.entrySet()) {
                  EncryptedShare share = new EncryptedShare();
                  share.data = entry.getValue().get("data");
                  share.iv = entry.getValue().get("iv");
                  encryptedPDF.encryptedShares.put(entry.getKey(), share);
              }

              // PDF 복호화
              byte[] decryptedPdf =
                      encryptionUtil.decryptPDF(encryptedPDF, ownerPassword, tenantPassword);

              // 파일명 생성
              String contractChatId = (String) metadata.get("contractChatId");
              String fileName =
                      contractChatId != null
                              ? contractChatId + "_decrypted.pdf"
                              : "decrypted_contract.pdf";

              // 복호화된 PDF 반환
              response.setContentType("application/pdf");
              response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
              response.setHeader("X-Decryption-Status", "SUCCESS");
              response.setContentLength(decryptedPdf.length);

              response.getOutputStream().write(decryptedPdf);
              response.getOutputStream().flush();

              log.info("Contract decrypted successfully: {}", fileName);

          } catch (IllegalArgumentException e) {
              log.error("Decryption failed - insufficient keys: {}", e.getMessage());
              try {
                  response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                  response.setContentType("application/json");
                  Map<String, Object> error = new HashMap<>();
                  error.put("success", false);
                  error.put("error", e.getMessage());
                  error.put(
                          "hint", "At least 2 keys (server + owner/tenant) required for decryption");
                  response.getWriter().write(objectMapper.writeValueAsString(error));
              } catch (IOException ioException) {
                  log.error("Failed to write error response: {}", ioException.getMessage());
              }
          } catch (Exception e) {
              log.error("Contract decryption failed: {}", e.getMessage(), e);
              try {
                  response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                  response.setContentType("application/json");
                  Map<String, Object> error = new HashMap<>();
                  error.put("success", false);
                  error.put("error", e.getMessage());
                  response.getWriter().write(objectMapper.writeValueAsString(error));
              } catch (IOException ioException) {
                  log.error("Failed to write error response: {}", ioException.getMessage());
              }
          }
      }
}
