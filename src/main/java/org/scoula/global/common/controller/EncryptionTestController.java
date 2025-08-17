package org.scoula.global.common.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.scoula.global.common.dto.FileWithHashDto;
import org.scoula.global.common.service.EncryptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/** 암호화/복호화 테스트 컨트롤러 - 이미지와 PDF 파일의 암호화 및 복호화 기능 제공 */
@RestController
@RequestMapping("/api/test/encryption")
@RequiredArgsConstructor
@Log4j2
public class EncryptionTestController {

      @Autowired private final EncryptionService encryptionService;

      // ==================== 이미지 암호화/복호화 ====================

      /** 1. 이미지 암호화 - 서버 키로 AES 암호화 */
      @PostMapping("/image/encrypt")
      public void encryptImage(
              @RequestParam("file") MultipartFile imageFile, HttpServletResponse response) {
          FileWithHashDto result = null;
          try {
              // 서비스 호출
              result = encryptionService.encryptImage(imageFile);
              File encryptedFile = result.getFile();

              String encryptedFilename = result.getFileName();

              // 응답 헤더에 메타데이터 추가
              response.setHeader("X-Original-Filename", imageFile.getOriginalFilename());
              response.setHeader("X-Encrypted-Filename", encryptedFilename);
              response.setHeader("X-Original-Hash", result.getOriginalHash());

              // 파일 다운로드 헤더 설정
              response.setContentType("application/octet-stream");
              response.setHeader(
                      "Content-Disposition", "attachment; filename=\"" + encryptedFilename + "\"");
              response.setContentLength((int) encryptedFile.length());

              // 파일 전송
              try (FileInputStream fis = new FileInputStream(encryptedFile)) {
                  byte[] buffer = new byte[4096];
                  int bytesRead;
                  while ((bytesRead = fis.read(buffer)) != -1) {
                      response.getOutputStream().write(buffer, 0, bytesRead);
                  }
                  response.getOutputStream().flush();
              }

              log.info("Image encrypted successfully - size: {} bytes", encryptedFile.length());

          } catch (Exception e) {
              log.error("Failed to encrypt image: {}", e.getMessage(), e);
              response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
              try {
                  response.getWriter().write("Error: " + e.getMessage());
              } catch (IOException ioException) {
                  log.error("Failed to write error response", ioException);
              }
          } finally {
              // 임시 파일 삭제
              if (result != null && result.getFile() != null && result.getFile().exists()) {
                  result.getFile().delete();
              }
          }
      }

      /** 2. 이미지 복호화 - 서버 키로 AES 복호화 */
      @PostMapping("/image/decrypt")
      public void decryptImage(
              @RequestParam("file") MultipartFile encryptedFile,
              @RequestParam(value = "originalHash", required = false) String originalHash,
              HttpServletResponse response) {
          File decryptedFile = null;
          try {
              // 서비스 호출 (salt와 iv는 파일에 포함되어 있음)
              decryptedFile = encryptionService.decryptImage(encryptedFile, originalHash);

              // 파일명 생성
              String fileName =
                      encryptedFile
                              .getOriginalFilename()
                              .replace("_encrypted.enc", "")
                              .replace(".enc", "");

              // Content-Type 추론
              String contentType = Files.probeContentType(decryptedFile.toPath());
              if (contentType == null) {
                  contentType = "application/octet-stream";
              }

              // 응답 헤더 설정
              response.setContentType(contentType);
              response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
              response.setContentLength((int) decryptedFile.length());

              // 파일 전송
              try (FileInputStream fis = new FileInputStream(decryptedFile)) {
                  byte[] buffer = new byte[4096];
                  int bytesRead;
                  while ((bytesRead = fis.read(buffer)) != -1) {
                      response.getOutputStream().write(buffer, 0, bytesRead);
                  }
                  response.getOutputStream().flush();
              }

              log.info("Image decrypted successfully - size: {} bytes", decryptedFile.length());

          } catch (SecurityException e) {
              log.error("Security error during decryption: {}", e.getMessage());
              response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
              try {
                  response.getWriter().write("Security Error: " + e.getMessage());
              } catch (IOException ioException) {
                  log.error("Failed to write error response", ioException);
              }
          } catch (Exception e) {
              log.error("Failed to decrypt image: {}", e.getMessage(), e);
              response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
              try {
                  response.getWriter().write("Error: " + e.getMessage());
              } catch (IOException ioException) {
                  log.error("Failed to write error response", ioException);
              }
          } finally {
              // 임시 파일 삭제
              if (decryptedFile != null && decryptedFile.exists()) {
                  decryptedFile.delete();
              }
          }
      }

      // ==================== PDF 암호화/복호화 ====================

      /** 3. PDF 암호화 1차 - Redis에 첫 번째 패스워드만 저장 */
      @PostMapping("/pdf/encrypt-step1")
      public ResponseEntity<?> encryptPDFStep1(
              @RequestParam("contractChatId") String contractChatId,
              @RequestParam("password1") String password1) {
          try {
              // 서비스 호출 (PDF 파일 없이)
              String message = encryptionService.uploadPdfStep1(contractChatId, password1);

              Map<String, Object> response = new HashMap<>();
              response.put("status", "success");
              response.put("message", message);
              response.put("contractChatId", contractChatId);
              response.put("step", 1);

              return ResponseEntity.ok(response);

          } catch (Exception e) {
              log.error("Failed to process PDF encryption step 1: {}", e.getMessage(), e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(Map.of("status", "error", "message", e.getMessage()));
          }
      }

      /** 4. PDF 암호화 2차 - PDF 파일 업로드 및 봉투키 암호화로 최종 암호화 파일 생성 */
      @PostMapping("/pdf/encrypt-step2")
      public void encryptPDFStep2(
              @RequestParam("file") MultipartFile pdfFile,
              @RequestParam("contractChatId") String contractChatId,
              @RequestParam("password2") String password2,
              HttpServletResponse response) {
          FileWithHashDto result = null;
          try {
              // 서비스 호출 (PDF 파일 포함)
              result = encryptionService.encryptPdfStep2(pdfFile, contractChatId, password2);
              File encryptedPdf = result.getFile();

              String encryptedFilename = result.getFileName();

              // 응답 헤더에 메타데이터 추가
              response.setHeader("X-Contract-Chat-Id", contractChatId);
              response.setHeader("X-Threshold", "2");
              response.setHeader("X-Total-Shares", "3");
              response.setHeader("X-Encrypted-Filename", encryptedFilename);
              if (result.getOriginalHash() != null) {
                  response.setHeader("X-Original-Hash", result.getOriginalHash());
              }

              // 파일 다운로드 헤더 설정
              response.setContentType("application/pdf");
              response.setHeader(
                      "Content-Disposition", "attachment; filename=\"" + encryptedFilename + "\"");
              response.setContentLength((int) encryptedPdf.length());

              // 파일 전송
              try (FileInputStream fis = new FileInputStream(encryptedPdf)) {
                  byte[] buffer = new byte[4096];
                  int bytesRead;
                  while ((bytesRead = fis.read(buffer)) != -1) {
                      response.getOutputStream().write(buffer, 0, bytesRead);
                  }
                  response.getOutputStream().flush();
              }

              log.info(
                      "PDF encrypted successfully - size: {} bytes, threshold: 2-of-3, hash: {}",
                      encryptedPdf.length(),
                      result.getOriginalHash());

          } catch (IllegalStateException e) {
              // 암호화가 아직 준비되지 않은 경우
              response.setStatus(HttpServletResponse.SC_ACCEPTED);
              try {
                  response.getWriter().write("Status: " + e.getMessage());
              } catch (IOException ioException) {
                  log.error("Failed to write response", ioException);
              }
          } catch (Exception e) {
              log.error("Failed to process PDF encryption step 2: {}", e.getMessage(), e);
              response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
              try {
                  response.getWriter().write("Error: " + e.getMessage());
              } catch (IOException ioException) {
                  log.error("Failed to write error response", ioException);
              }
          } finally {
              // 임시 파일 삭제
              if (result != null && result.getFile() != null && result.getFile().exists()) {
                  result.getFile().delete();
              }
          }
      }

      /** 5. PDF 복호화 - 2-of-3 방식으로 암호화된 PDF 복호화 */
      @PostMapping("/pdf/decrypt")
      public void decryptPDF(
              @RequestParam("file") MultipartFile encryptedPdfFile,
              @RequestParam("password") String password,
              @RequestParam(value = "originalHash", required = false) String originalHash,
              HttpServletResponse response) {
          File decryptedPdf = null;
          try {
              // 서비스 호출
              decryptedPdf = encryptionService.decryptPdf(encryptedPdfFile, password, originalHash);

              // 파일명 생성
              String fileName =
                      encryptedPdfFile
                              .getOriginalFilename()
                              .replace("_encrypted", "")
                              .replaceFirst("(\\.[^.]*)?$", "_decrypted.pdf");

              // 응답 헤더 설정
              response.setContentType("application/pdf");
              response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
              response.setContentLength((int) decryptedPdf.length());

              // 파일 전송
              try (FileInputStream fis = new FileInputStream(decryptedPdf)) {
                  byte[] buffer = new byte[4096];
                  int bytesRead;
                  while ((bytesRead = fis.read(buffer)) != -1) {
                      response.getOutputStream().write(buffer, 0, bytesRead);
                  }
                  response.getOutputStream().flush();
              }

              log.info("PDF decrypted successfully - size: {} bytes", decryptedPdf.length());

          } catch (SecurityException e) {
              log.error("Security error during decryption: {}", e.getMessage());
              response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
              try {
                  response.getWriter().write("Security Error: " + e.getMessage());
              } catch (IOException ioException) {
                  log.error("Failed to write error response", ioException);
              }
          } catch (IllegalArgumentException e) {
              // 잘못된 패스워드나 입력값 에러는 400 Bad Request
              log.error("Invalid input for PDF decryption: {}", e.getMessage());
              response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
              try {
                  response.getWriter().write("Error: " + e.getMessage());
              } catch (IOException ioException) {
                  log.error("Failed to write error response", ioException);
              }
          } catch (Exception e) {
              log.error("Failed to decrypt PDF: {}", e.getMessage(), e);
              response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
              try {
                  response.getWriter().write("Error: " + e.getMessage());
              } catch (IOException ioException) {
                  log.error("Failed to write error response", ioException);
              }
          } finally {
              // 임시 파일 삭제
              if (decryptedPdf != null && decryptedPdf.exists()) {
                  decryptedPdf.delete();
              }
          }
      }

      /** 6. PDF 비밀번호 걸기 - 단순 패스워드 방식의 PDF 암호화 (편집 권한 제한 없음) */
      @PostMapping("/pdf/password-protect")
      public void passwordProtectPDF(
              @RequestParam("file") MultipartFile pdfFile,
              @RequestParam("password") String password,
              HttpServletResponse response) {
          FileWithHashDto result = null;
          try {
              // 서비스 호출
              result = encryptionService.addPasswordToPdf(pdfFile, password);
              File protectedPdf = result.getFile();

              // 파일명 생성
              String fileName =
                      pdfFile.getOriginalFilename().replaceFirst("(\\.[^.]*)?$", "_protected.pdf");

              // 응답 헤더 설정
              response.setContentType("application/pdf");
              response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
              response.setContentLength((int) protectedPdf.length());
              response.setHeader("X-Original-Hash", result.getOriginalHash());

              // 파일 전송
              try (FileInputStream fis = new FileInputStream(protectedPdf)) {
                  byte[] buffer = new byte[4096];
                  int bytesRead;
                  while ((bytesRead = fis.read(buffer)) != -1) {
                      response.getOutputStream().write(buffer, 0, bytesRead);
                  }
                  response.getOutputStream().flush();
              }

              log.info("PDF password protected successfully - size: {} bytes", protectedPdf.length());

          } catch (Exception e) {
              log.error("Failed to password protect PDF: {}", e.getMessage(), e);
              response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
              try {
                  response.getWriter().write("Error: " + e.getMessage());
              } catch (IOException ioException) {
                  log.error("Failed to write error response", ioException);
              }
          } finally {
              // 임시 파일 삭제
              if (result != null && result.getFile() != null && result.getFile().exists()) {
                  result.getFile().delete();
              }
          }
      }

      // ==================== 유틸리티 엔드포인트 ====================

      /** 암호화 테스트 API 상태 확인 */
      @GetMapping("/status")
      public ResponseEntity<?> getStatus() {
          Map<String, Object> status = new HashMap<>();
          status.put("status", "active");
          status.put("service", "Encryption Test API");
          status.put(
                  "endpoints",
                  Map.of(
                          "image",
                                  new String[] {
                                      "/api/test/encryption/image/encrypt",
                                      "/api/test/encryption/image/decrypt"
                                  },
                          "pdf",
                                  new String[] {
                                      "/api/test/encryption/pdf/encrypt-step1",
                                      "/api/test/encryption/pdf/encrypt-step2",
                                      "/api/test/encryption/pdf/decrypt",
                                      "/api/test/encryption/pdf/password-protect"
                                  }));

          return ResponseEntity.ok(status);
      }
}
