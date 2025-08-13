package org.scoula.global.common.util.encryptionTest.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.scoula.global.common.util.EncryptionUtil;
import org.scoula.global.common.util.EncryptionUtil.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/** 계약서 복호화 컨트롤러 - 암호화된 파일을 업로드하여 복호화 */
@RestController
@RequestMapping("/api/contract/decryption")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Log4j2
public class ContractDecryptionController {

      private final EncryptionUtil encryptionUtil;
      private final ObjectMapper objectMapper = new ObjectMapper();

      /** 암호화된 계약서 복호화 - Owner 키 사용 (서버 키와 결합) */
      @PostMapping("/decrypt/owner")
      public void decryptWithOwnerKey(
              @RequestParam("contractChatId") String contractChatId,
              @RequestParam("encryptedFile") MultipartFile encryptedFile,
              @RequestParam("ownerPassword") String ownerPassword,
              HttpServletResponse response) {

          try {
              log.info("Decryption request with owner key for contractChatId: {}", contractChatId);

              // 암호화된 파일 파싱
              byte[] fileData = encryptedFile.getBytes();
              String fileContent =
                      new String(fileData, java.nio.charset.StandardCharsets.UTF_8).trim();

              log.debug(
                      "File content first 100 chars: {}",
                      fileContent.length() > 100
                              ? fileContent.substring(0, 100) + "..."
                              : fileContent);

              // JSON 파싱 - 다양한 형식 처리
              Map<String, Object> encryptedFileData;

              try {
                  if (fileContent.startsWith("{")) {
                      // 일반 JSON
                      encryptedFileData = objectMapper.readValue(fileContent, Map.class);
                  } else if (fileContent.startsWith("\"") && fileContent.endsWith("\"")) {
                      // JSON 문자열로 래핑된 경우
                      String unwrapped = objectMapper.readValue(fileContent, String.class);
                      // 언래핑된 내용이 Base64인지 JSON인지 확인
                      if (unwrapped.startsWith("{")) {
                          encryptedFileData = objectMapper.readValue(unwrapped, Map.class);
                      } else {
                          // Base64로 인코딩된 JSON
                          byte[] decodedBytes = java.util.Base64.getDecoder().decode(unwrapped);
                          String decodedContent =
                                  new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8);
                          encryptedFileData = objectMapper.readValue(decodedContent, Map.class);
                      }
                  } else {
                      // Base64로 인코딩된 JSON (직접)
                      try {
                          byte[] decodedBytes = java.util.Base64.getDecoder().decode(fileContent);
                          String decodedContent =
                                  new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8);
                          encryptedFileData = objectMapper.readValue(decodedContent, Map.class);
                      } catch (IllegalArgumentException base64Error) {
                          // Base64가 아닌 경우 직접 파싱 시도
                          encryptedFileData = objectMapper.readValue(fileContent, Map.class);
                      }
                  }
              } catch (Exception e) {
                  log.error("Failed to parse encrypted file: {}", e.getMessage());
                  throw new IllegalArgumentException(
                          "Invalid encrypted file format. Please upload a valid encrypted JSON"
                                  + " file.");
              }

              // 암호화된 데이터 추출
              String encryptedDataBase64 = (String) encryptedFileData.get("encryptedData");
              byte[] encryptedData = java.util.Base64.getDecoder().decode(encryptedDataBase64);

              // 메타데이터 추출
              Map<String, Object> metadata = (Map<String, Object>) encryptedFileData.get("metadata");

              // EncryptedPDF 객체 재구성
              EncryptedPDF encryptedPDF = new EncryptedPDF();
              encryptedPDF.encryptedData = encryptedData;
              encryptedPDF.iv = (String) metadata.get("iv");
              encryptedPDF.originalHash = (String) metadata.get("originalHash");
              encryptedPDF.threshold = (Integer) metadata.get("threshold");
              encryptedPDF.totalShares = (Integer) metadata.get("totalShares");

              // Share 정보 재구성
              Map<String, Map<String, String>> sharesMap =
                      (Map<String, Map<String, String>>) metadata.get("encryptedShares");
              Map<String, EncryptedShare> encryptedShares = new HashMap<>();

              for (Map.Entry<String, Map<String, String>> entry : sharesMap.entrySet()) {
                  EncryptedShare share = new EncryptedShare();
                  share.data = entry.getValue().get("data");
                  share.iv = entry.getValue().get("iv");
                  encryptedShares.put(entry.getKey(), share);
              }
              encryptedPDF.encryptedShares = encryptedShares;

              // PDF 복호화 (서버 키 + Owner 키)
              byte[] decryptedPDF = encryptionUtil.decryptPDF(encryptedPDF, ownerPassword, null);

              // 복호화된 PDF 반환
              String fileName = contractChatId + "_decrypted.pdf";

              response.setContentType("application/pdf");
              response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
              response.setHeader("X-Contract-Chat-Id", contractChatId);
              response.setHeader("X-Decryption-Method", "OWNER_KEY");
              response.setHeader("X-Status", "SUCCESS");
              response.setContentLength(decryptedPDF.length);

              response.getOutputStream().write(decryptedPDF);
              response.getOutputStream().flush();

          } catch (Exception e) {
              log.error("Decryption failed: {}", e.getMessage(), e);
              try {
                  response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                  response.setContentType("application/json");
                  Map<String, Object> error = new HashMap<>();
                  error.put("success", false);
                  error.put("contractChatId", contractChatId);
                  error.put("error", e.getMessage());
                  if (e instanceof IllegalArgumentException) {
                      error.put("requiredKeys", "Need at least 2 keys (server + owner)");
                  } else if (e instanceof SecurityException) {
                      error.put("error", "Hash verification failed: " + e.getMessage());
                  }
                  response.getWriter().write(objectMapper.writeValueAsString(error));
              } catch (IOException ioException) {
                  log.error("Failed to write error response: {}", ioException.getMessage());
              }
          }
      }

      /** 암호화된 계약서 복호화 - Tenant 키 사용 (서버 키와 결합) */
      @PostMapping("/decrypt/tenant")
      public void decryptWithTenantKey(
              @RequestParam("contractChatId") String contractChatId,
              @RequestParam("encryptedFile") MultipartFile encryptedFile,
              @RequestParam("tenantPassword") String tenantPassword,
              HttpServletResponse response) {

          try {
              log.info("Decryption request with tenant key for contractChatId: {}", contractChatId);

              // 암호화된 파일 파싱
              byte[] fileData = encryptedFile.getBytes();
              String fileContent =
                      new String(fileData, java.nio.charset.StandardCharsets.UTF_8).trim();

              log.debug(
                      "File content first 100 chars: {}",
                      fileContent.length() > 100
                              ? fileContent.substring(0, 100) + "..."
                              : fileContent);

              // JSON 파싱 - 다양한 형식 처리
              Map<String, Object> encryptedFileData;

              try {
                  if (fileContent.startsWith("{")) {
                      // 일반 JSON
                      encryptedFileData = objectMapper.readValue(fileContent, Map.class);
                  } else if (fileContent.startsWith("\"") && fileContent.endsWith("\"")) {
                      // JSON 문자열로 래핑된 경우
                      String unwrapped = objectMapper.readValue(fileContent, String.class);
                      // 언래핑된 내용이 Base64인지 JSON인지 확인
                      if (unwrapped.startsWith("{")) {
                          encryptedFileData = objectMapper.readValue(unwrapped, Map.class);
                      } else {
                          // Base64로 인코딩된 JSON
                          byte[] decodedBytes = java.util.Base64.getDecoder().decode(unwrapped);
                          String decodedContent =
                                  new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8);
                          encryptedFileData = objectMapper.readValue(decodedContent, Map.class);
                      }
                  } else {
                      // Base64로 인코딩된 JSON (직접)
                      try {
                          byte[] decodedBytes = java.util.Base64.getDecoder().decode(fileContent);
                          String decodedContent =
                                  new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8);
                          encryptedFileData = objectMapper.readValue(decodedContent, Map.class);
                      } catch (IllegalArgumentException base64Error) {
                          // Base64가 아닌 경우 직접 파싱 시도
                          encryptedFileData = objectMapper.readValue(fileContent, Map.class);
                      }
                  }
              } catch (Exception e) {
                  log.error("Failed to parse encrypted file: {}", e.getMessage());
                  throw new IllegalArgumentException(
                          "Invalid encrypted file format. Please upload a valid encrypted JSON"
                                  + " file.");
              }

              // 암호화된 데이터 추출
              String encryptedDataBase64 = (String) encryptedFileData.get("encryptedData");
              byte[] encryptedData = java.util.Base64.getDecoder().decode(encryptedDataBase64);

              // 메타데이터 추출
              Map<String, Object> metadata = (Map<String, Object>) encryptedFileData.get("metadata");

              // EncryptedPDF 객체 재구성
              EncryptedPDF encryptedPDF = new EncryptedPDF();
              encryptedPDF.encryptedData = encryptedData;
              encryptedPDF.iv = (String) metadata.get("iv");
              encryptedPDF.originalHash = (String) metadata.get("originalHash");
              encryptedPDF.threshold = (Integer) metadata.get("threshold");
              encryptedPDF.totalShares = (Integer) metadata.get("totalShares");

              // Share 정보 재구성
              Map<String, Map<String, String>> sharesMap =
                      (Map<String, Map<String, String>>) metadata.get("encryptedShares");
              Map<String, EncryptedShare> encryptedShares = new HashMap<>();

              for (Map.Entry<String, Map<String, String>> entry : sharesMap.entrySet()) {
                  EncryptedShare share = new EncryptedShare();
                  share.data = entry.getValue().get("data");
                  share.iv = entry.getValue().get("iv");
                  encryptedShares.put(entry.getKey(), share);
              }
              encryptedPDF.encryptedShares = encryptedShares;

              // PDF 복호화 (서버 키 + Tenant 키)
              byte[] decryptedPDF = encryptionUtil.decryptPDF(encryptedPDF, null, tenantPassword);

              // 복호화된 PDF 반환
              String fileName = contractChatId + "_decrypted.pdf";

              response.setContentType("application/pdf");
              response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
              response.setHeader("X-Contract-Chat-Id", contractChatId);
              response.setHeader("X-Decryption-Method", "TENANT_KEY");
              response.setHeader("X-Status", "SUCCESS");
              response.setContentLength(decryptedPDF.length);

              response.getOutputStream().write(decryptedPDF);
              response.getOutputStream().flush();

          } catch (Exception e) {
              log.error("Decryption failed: {}", e.getMessage(), e);
              try {
                  response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                  response.setContentType("application/json");
                  Map<String, Object> error = new HashMap<>();
                  error.put("success", false);
                  error.put("contractChatId", contractChatId);
                  error.put("error", e.getMessage());
                  if (e instanceof IllegalArgumentException) {
                      error.put("requiredKeys", "Need at least 2 keys (server + tenant)");
                  } else if (e instanceof SecurityException) {
                      error.put("error", "Hash verification failed: " + e.getMessage());
                  }
                  response.getWriter().write(objectMapper.writeValueAsString(error));
              } catch (IOException ioException) {
                  log.error("Failed to write error response: {}", ioException.getMessage());
              }
          }
      }

      /** 암호화된 계약서 복호화 - 두 키 모두 사용 (더 안전한 옵션) */
      @PostMapping("/decrypt/both")
      public void decryptWithBothKeys(
              @RequestParam("contractChatId") String contractChatId,
              @RequestParam("encryptedFile") MultipartFile encryptedFile,
              @RequestParam("ownerPassword") String ownerPassword,
              @RequestParam("tenantPassword") String tenantPassword,
              HttpServletResponse response) {

          try {
              log.info("Decryption request with both keys for contractChatId: {}", contractChatId);

              // 암호화된 파일 파싱
              byte[] fileData = encryptedFile.getBytes();
              String fileContent =
                      new String(fileData, java.nio.charset.StandardCharsets.UTF_8).trim();

              log.debug(
                      "File content first 100 chars: {}",
                      fileContent.length() > 100
                              ? fileContent.substring(0, 100) + "..."
                              : fileContent);

              // JSON 파싱 - 다양한 형식 처리
              Map<String, Object> encryptedFileData;

              try {
                  if (fileContent.startsWith("{")) {
                      // 일반 JSON
                      encryptedFileData = objectMapper.readValue(fileContent, Map.class);
                  } else if (fileContent.startsWith("\"") && fileContent.endsWith("\"")) {
                      // JSON 문자열로 래핑된 경우
                      String unwrapped = objectMapper.readValue(fileContent, String.class);
                      // 언래핑된 내용이 Base64인지 JSON인지 확인
                      if (unwrapped.startsWith("{")) {
                          encryptedFileData = objectMapper.readValue(unwrapped, Map.class);
                      } else {
                          // Base64로 인코딩된 JSON
                          byte[] decodedBytes = java.util.Base64.getDecoder().decode(unwrapped);
                          String decodedContent =
                                  new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8);
                          encryptedFileData = objectMapper.readValue(decodedContent, Map.class);
                      }
                  } else {
                      // Base64로 인코딩된 JSON (직접)
                      try {
                          byte[] decodedBytes = java.util.Base64.getDecoder().decode(fileContent);
                          String decodedContent =
                                  new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8);
                          encryptedFileData = objectMapper.readValue(decodedContent, Map.class);
                      } catch (IllegalArgumentException base64Error) {
                          // Base64가 아닌 경우 직접 파싱 시도
                          encryptedFileData = objectMapper.readValue(fileContent, Map.class);
                      }
                  }
              } catch (Exception e) {
                  log.error("Failed to parse encrypted file: {}", e.getMessage());
                  throw new IllegalArgumentException(
                          "Invalid encrypted file format. Please upload a valid encrypted JSON"
                                  + " file.");
              }

              // 암호화된 데이터 추출
              String encryptedDataBase64 = (String) encryptedFileData.get("encryptedData");
              byte[] encryptedData = java.util.Base64.getDecoder().decode(encryptedDataBase64);

              // 메타데이터 추출
              Map<String, Object> metadata = (Map<String, Object>) encryptedFileData.get("metadata");

              // EncryptedPDF 객체 재구성
              EncryptedPDF encryptedPDF = new EncryptedPDF();
              encryptedPDF.encryptedData = encryptedData;
              encryptedPDF.iv = (String) metadata.get("iv");
              encryptedPDF.originalHash = (String) metadata.get("originalHash");
              encryptedPDF.threshold = (Integer) metadata.get("threshold");
              encryptedPDF.totalShares = (Integer) metadata.get("totalShares");

              // Share 정보 재구성
              Map<String, Map<String, String>> sharesMap =
                      (Map<String, Map<String, String>>) metadata.get("encryptedShares");
              Map<String, EncryptedShare> encryptedShares = new HashMap<>();

              for (Map.Entry<String, Map<String, String>> entry : sharesMap.entrySet()) {
                  EncryptedShare share = new EncryptedShare();
                  share.data = entry.getValue().get("data");
                  share.iv = entry.getValue().get("iv");
                  encryptedShares.put(entry.getKey(), share);
              }
              encryptedPDF.encryptedShares = encryptedShares;

              // PDF 복호화 (Owner 키 + Tenant 키)
              byte[] decryptedPDF =
                      encryptionUtil.decryptPDF(encryptedPDF, ownerPassword, tenantPassword);

              // 복호화된 PDF 반환
              String fileName = contractChatId + "_decrypted.pdf";

              response.setContentType("application/pdf");
              response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
              response.setHeader("X-Contract-Chat-Id", contractChatId);
              response.setHeader("X-Decryption-Method", "BOTH_KEYS");
              response.setHeader("X-Status", "SUCCESS");
              response.setContentLength(decryptedPDF.length);

              response.getOutputStream().write(decryptedPDF);
              response.getOutputStream().flush();

          } catch (Exception e) {
              log.error("Decryption failed: {}", e.getMessage(), e);
              try {
                  response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                  response.setContentType("application/json");
                  Map<String, Object> error = new HashMap<>();
                  error.put("success", false);
                  error.put("contractChatId", contractChatId);
                  error.put("error", e.getMessage());
                  if (e instanceof IllegalArgumentException) {
                      error.put("requiredKeys", "Need at least 2 keys");
                  } else if (e instanceof SecurityException) {
                      error.put("error", "Hash verification failed: " + e.getMessage());
                  }
                  response.getWriter().write(objectMapper.writeValueAsString(error));
              } catch (IOException ioException) {
                  log.error("Failed to write error response: {}", ioException.getMessage());
              }
          }
      }
}
