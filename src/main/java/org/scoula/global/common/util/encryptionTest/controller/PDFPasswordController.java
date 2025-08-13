package org.scoula.global.common.util.encryptionTest.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.scoula.global.common.util.EncryptionUtil;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/** PDF 직접 암호화 컨트롤러 - iText를 사용한 PDF 암호 설정 */
@RestController
@RequestMapping("/api/pdf/password")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Log4j2
public class PDFPasswordController {

      /** PDF 파일에 암호 설정 (편집 불가능) */
      @PostMapping("/encrypt")
      public void encryptPDFWithPassword(
              @RequestParam("file") MultipartFile pdfFile,
              @RequestParam("password") String password,
              HttpServletResponse response) {

          try {
              log.info("PDF password encryption request for file: {}", pdfFile.getOriginalFilename());

              // PDF 파일 검증
              if (pdfFile.isEmpty()) {
                  throw new IllegalArgumentException("PDF file is empty");
              }

              String contentType = pdfFile.getContentType();
              if (contentType == null || !contentType.equals("application/pdf")) {
                  throw new IllegalArgumentException("File must be a PDF document");
              }

              // PDF 데이터 읽기
              byte[] pdfData = pdfFile.getBytes();
              log.info("Original PDF size: {} bytes", pdfData.length);

              // PDF 암호화 (편집 제한 적용)
              byte[] encryptedPdf = EncryptionUtil.encryptPDFWithEditRestriction(pdfData, password);
              log.info("PDF encrypted with edit restrictions applied");

              // 파일명 생성
              String originalFileName = pdfFile.getOriginalFilename();
              String fileName =
                      originalFileName != null
                              ? originalFileName.replaceFirst("(\\.[^.]*)?$", "_encrypted.pdf")
                              : "encrypted_document.pdf";

              // 암호화된 PDF 반환
              response.setContentType("application/pdf");
              response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
              response.setHeader("X-Encryption-Type", "PDF_PASSWORD");
              response.setHeader("X-Status", "SUCCESS");
              response.setContentLength(encryptedPdf.length);

              response.getOutputStream().write(encryptedPdf);
              response.getOutputStream().flush();

              log.info("Encrypted PDF returned successfully, filename: {}", fileName);

          } catch (Exception e) {
              log.error("PDF password encryption failed: {}", e.getMessage(), e);
              try {
                  response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                  response.setContentType("application/json");
                  Map<String, Object> error = new HashMap<>();
                  error.put("success", false);
                  error.put("error", e.getMessage());
                  if (e instanceof IllegalArgumentException) {
                      error.put("type", "VALIDATION_ERROR");
                  } else {
                      error.put("type", "ENCRYPTION_ERROR");
                  }
                  response.getWriter()
                          .write(
                                  new com.fasterxml.jackson.databind.ObjectMapper()
                                          .writeValueAsString(error));
              } catch (IOException ioException) {
                  log.error("Failed to write error response: {}", ioException.getMessage());
              }
          }
      }

      /** PDF 파일 암호 해제 - 암호로 보호된 PDF를 받아서 암호를 해제 */
      @PostMapping("/decrypt")
      public void decryptPDFWithPassword(
              @RequestParam("file") MultipartFile encryptedPdfFile,
              @RequestParam("password") String password,
              HttpServletResponse response) {

          try {
              log.info(
                      "PDF password decryption request for file: {}",
                      encryptedPdfFile.getOriginalFilename());

              // PDF 파일 검증
              if (encryptedPdfFile.isEmpty()) {
                  throw new IllegalArgumentException("PDF file is empty");
              }

              String contentType = encryptedPdfFile.getContentType();
              if (contentType == null || !contentType.equals("application/pdf")) {
                  throw new IllegalArgumentException("File must be a PDF document");
              }

              // 암호화된 PDF 데이터 읽기
              byte[] encryptedPdfData = encryptedPdfFile.getBytes();
              log.info("Encrypted PDF size: {} bytes", encryptedPdfData.length);

              // 암호화 상태 확인
              if (!EncryptionUtil.isPDFEncrypted(encryptedPdfData)) {
                  log.warn("PDF is not encrypted, returning original file");

                  // 암호화되지 않은 경우 원본 그대로 반환
                  String originalFileName = encryptedPdfFile.getOriginalFilename();
                  response.setContentType("application/pdf");
                  response.setHeader(
                          "Content-Disposition", "attachment; filename=\"" + originalFileName + "\"");
                  response.setHeader("X-Decryption-Status", "NOT_ENCRYPTED");
                  response.setContentLength(encryptedPdfData.length);

                  response.getOutputStream().write(encryptedPdfData);
                  response.getOutputStream().flush();
                  return;
              }

              // PDF 복호화 (암호 해제)
              byte[] decryptedPdf = EncryptionUtil.decryptPDFWithPassword(encryptedPdfData, password);
              log.info("PDF decrypted successfully, size: {} bytes", decryptedPdf.length);

              // 파일명 생성
              String originalFileName = encryptedPdfFile.getOriginalFilename();
              String fileName =
                      originalFileName != null
                              ? originalFileName.replaceFirst("(\\.[^.]*)?$", "_decrypted.pdf")
                              : "decrypted_document.pdf";

              // 복호화된 PDF 반환
              response.setContentType("application/pdf");
              response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
              response.setHeader("X-Decryption-Status", "SUCCESS");
              response.setContentLength(decryptedPdf.length);

              response.getOutputStream().write(decryptedPdf);
              response.getOutputStream().flush();

              log.info("Decrypted PDF returned successfully, filename: {}", fileName);

          } catch (IllegalArgumentException e) {
              log.error("PDF decryption validation error: {}", e.getMessage());
              try {
                  response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                  response.setContentType("application/json");
                  Map<String, Object> error = new HashMap<>();
                  error.put("success", false);
                  error.put("error", e.getMessage());
                  error.put("type", "VALIDATION_ERROR");
                  response.getWriter()
                          .write(
                                  new com.fasterxml.jackson.databind.ObjectMapper()
                                          .writeValueAsString(error));
              } catch (IOException ioException) {
                  log.error("Failed to write error response: {}", ioException.getMessage());
              }
          } catch (Exception e) {
              log.error("PDF password decryption failed: {}", e.getMessage(), e);
              try {
                  response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                  response.setContentType("application/json");
                  Map<String, Object> error = new HashMap<>();
                  error.put("success", false);
                  error.put("error", e.getMessage());
                  error.put("type", "DECRYPTION_ERROR");
                  error.put("hint", "Please check if the password is correct");
                  response.getWriter()
                          .write(
                                  new com.fasterxml.jackson.databind.ObjectMapper()
                                          .writeValueAsString(error));
              } catch (IOException ioException) {
                  log.error("Failed to write error response: {}", ioException.getMessage());
              }
          }
      }
}
