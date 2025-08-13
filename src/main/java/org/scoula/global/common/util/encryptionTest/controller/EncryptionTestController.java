package org.scoula.global.common.util.encryptionTest.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.scoula.global.common.util.EncryptionUtil;
import org.scoula.global.common.util.EncryptionUtil.EncryptedImage;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequestMapping("/api/encryption/test")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Log4j2
public class EncryptionTestController {

      private final EncryptionUtil encryptionUtil;

      /** 이미지 암호화 - 암호화된 파일을 다운로드로 반환 */
      @PostMapping("/image/encrypt")
      public void encryptImage(
              @RequestParam("file") MultipartFile file,
              @RequestParam("password") String password,
              HttpServletResponse response) {
          try {
              log.info("Encrypting image: {}", file.getOriginalFilename());

              byte[] imageData = file.getBytes();
              EncryptedImage encrypted = EncryptionUtil.encryptImage(imageData, password);

              // 암호화된 데이터를 JSON으로 구성
              Map<String, Object> encryptedFileData = new HashMap<>();
              encryptedFileData.put("success", true);
              encryptedFileData.put(
                      "encryptedData",
                      org.apache.commons.codec.binary.Base64.encodeBase64String(
                              encrypted.encryptedData));
              encryptedFileData.put("salt", encrypted.salt);
              encryptedFileData.put("iv", encrypted.iv);
              encryptedFileData.put("originalHash", encrypted.originalHash);
              encryptedFileData.put("algorithm", encrypted.algorithm);
              encryptedFileData.put("originalFileName", file.getOriginalFilename());

              // JSON으로 변환
              com.fasterxml.jackson.databind.ObjectMapper objectMapper =
                      new com.fasterxml.jackson.databind.ObjectMapper();
              byte[] jsonData = objectMapper.writeValueAsBytes(encryptedFileData);

              // 파일명 생성
              String originalFileName = file.getOriginalFilename();
              String fileName =
                      originalFileName != null
                              ? originalFileName.replaceFirst("(\\.[^.]*)?$", "_encrypted.json")
                              : "encrypted_image.json";

              // 암호화된 데이터를 파일로 다운로드
              response.setContentType("application/json");
              response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
              response.setHeader("X-Encryption-Type", "IMAGE_AES");
              response.setHeader(
                      "X-Original-File", originalFileName != null ? originalFileName : "unknown");
              response.setContentLength(jsonData.length);

              response.getOutputStream().write(jsonData);
              response.getOutputStream().flush();

              log.info("Encrypted image returned as file: {}", fileName);

          } catch (Exception e) {
              log.error("Image encryption failed: {}", e.getMessage(), e);
              try {
                  response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                  response.setContentType("application/json");
                  Map<String, Object> error = new HashMap<>();
                  error.put("success", false);
                  error.put("error", e.getMessage());
                  response.getWriter()
                          .write(
                                  new com.fasterxml.jackson.databind.ObjectMapper()
                                          .writeValueAsString(error));
              } catch (IOException ioException) {
                  log.error("Failed to write error response: {}", ioException.getMessage());
              }
          }
      }

      /** 이미지 복호화 - 암호화된 JSON 파일을 받아서 원본 이미지로 복호화 */
      @PostMapping("/image/decrypt")
      public void decryptImage(
              @RequestParam("file") MultipartFile encryptedFile,
              @RequestParam("password") String password,
              HttpServletResponse response) {
          try {
              log.info("Decrypting image file: {}", encryptedFile.getOriginalFilename());

              // JSON 파일 읽기
              byte[] jsonData = encryptedFile.getBytes();

              // JSON 파싱
              com.fasterxml.jackson.databind.ObjectMapper objectMapper =
                      new com.fasterxml.jackson.databind.ObjectMapper();
              Map<String, Object> encryptedMap = objectMapper.readValue(jsonData, Map.class);

              // EncryptedImage 객체 재구성
              EncryptedImage encrypted = new EncryptedImage();
              encrypted.encryptedData =
                      org.apache.commons.codec.binary.Base64.decodeBase64(
                              (String) encryptedMap.get("encryptedData"));
              encrypted.salt = (String) encryptedMap.get("salt");
              encrypted.iv = (String) encryptedMap.get("iv");
              encrypted.originalHash = (String) encryptedMap.get("originalHash");
              encrypted.algorithm = (String) encryptedMap.get("algorithm");

              // 복호화
              byte[] decryptedData = EncryptionUtil.decryptImage(encrypted, password);

              // 파일명 결정
              String originalFileName =
                      encryptedMap.containsKey("originalFileName")
                              ? (String) encryptedMap.get("originalFileName")
                              : "decrypted_image.png";

              // 파일 확장자 추출 (없으면 기본값 사용)
              String contentType = "application/octet-stream";
              if (originalFileName != null) {
                  String extension =
                          originalFileName
                                  .substring(originalFileName.lastIndexOf(".") + 1)
                                  .toLowerCase();
                  switch (extension) {
                      case "jpg":
                      case "jpeg":
                          contentType = "image/jpeg";
                          break;
                      case "png":
                          contentType = "image/png";
                          break;
                      case "gif":
                          contentType = "image/gif";
                          break;
                      case "bmp":
                          contentType = "image/bmp";
                          break;
                      case "webp":
                          contentType = "image/webp";
                          break;
                  }
              }

              // 응답 설정
              response.setContentType(contentType);
              response.setHeader(
                      "Content-Disposition", "attachment; filename=\"" + originalFileName + "\"");
              response.setHeader("X-Decryption-Status", "SUCCESS");
              response.setContentLength(decryptedData.length);

              // 데이터 전송
              response.getOutputStream().write(decryptedData);
              response.getOutputStream().flush();

              log.info("Image decrypted successfully: {}", originalFileName);

          } catch (Exception e) {
              log.error("Image decryption failed: {}", e.getMessage(), e);
              try {
                  response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                  response.setContentType("application/json");
                  Map<String, Object> error = new HashMap<>();
                  error.put("success", false);
                  error.put("error", e.getMessage());
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
