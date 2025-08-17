package org.scoula.global.common.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.scoula.global.common.dto.FileWithHashDto;
import org.scoula.global.common.util.EncryptionUtil;
import org.scoula.global.common.util.EncryptionUtil.ContractKeyStatus;
import org.scoula.global.common.util.EncryptionUtil.EncryptedImage;
import org.scoula.global.common.util.EncryptionUtil.EncryptedPDF;
import org.scoula.global.common.util.EncryptionUtil.EncryptedShare;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/** 암호화/복호화 서비스 구현체 */
@Service
@RequiredArgsConstructor
@Log4j2
public class EncryptionServiceImpl implements EncryptionService {

      @Autowired private final EncryptionUtil encryptionUtil;

      // ==================== 이미지 암호화/복호화 ====================

      @Override
      public FileWithHashDto encryptImage(MultipartFile imageFile) throws Exception {
          log.info("Encrypting image: {}", imageFile.getOriginalFilename());

          // 원본 파일의 해시값 계산
          byte[] originalData = imageFile.getBytes();
          String originalHash = DigestUtils.sha256Hex(originalData);

          // 이미지 암호화 (서버 마스터 키 사용)
          EncryptedImage encryptedImage = encryptionUtil.encryptImageFromMultipartFile(imageFile);

          // 원본 파일명과 확장자 추출
          String originalFilename = imageFile.getOriginalFilename();
          String nameWithoutExt = originalFilename;
          String originalExt = "";
          if (originalFilename != null && originalFilename.contains(".")) {
              int lastDotIndex = originalFilename.lastIndexOf(".");
              nameWithoutExt = originalFilename.substring(0, lastDotIndex);
              originalExt = originalFilename.substring(lastDotIndex); // ".jpg" 형태로 저장
          }

          // 파일명 생성 (원본 확장자를 파일명에 포함)
          String encryptedFilename = nameWithoutExt + originalExt + "_encrypted.enc";

          // salt와 iv를 암호화된 데이터 앞에 추가
          // 형식: [salt (Base64 decoded)][iv (Base64 decoded)][encrypted data]
          byte[] saltBytes = Base64.decodeBase64(encryptedImage.salt);
          byte[] ivBytes = Base64.decodeBase64(encryptedImage.iv);
          byte[] combinedData =
                  new byte[saltBytes.length + ivBytes.length + encryptedImage.encryptedData.length];

          System.arraycopy(saltBytes, 0, combinedData, 0, saltBytes.length);
          System.arraycopy(ivBytes, 0, combinedData, saltBytes.length, ivBytes.length);
          System.arraycopy(
                  encryptedImage.encryptedData,
                  0,
                  combinedData,
                  saltBytes.length + ivBytes.length,
                  encryptedImage.encryptedData.length);

          // 임시 파일 생성
          File tempFile = File.createTempFile("encrypted_", ".enc");
          tempFile.deleteOnExit(); // JVM 종료 시 자동 삭제

          // 파일에 쓰기
          try (FileOutputStream fos = new FileOutputStream(tempFile)) {
              fos.write(combinedData);
          }

          log.info(
                  "Image encrypted to temp file: {}, size: {} bytes, hash: {}, original extension:"
                          + " {}",
                  tempFile.getAbsolutePath(),
                  tempFile.length(),
                  originalHash,
                  originalExt);

          return FileWithHashDto.builder()
                  .file(tempFile)
                  .originalHash(originalHash)
                  .fileName(encryptedFilename)
                  .contentType("application/octet-stream")
                  .build();
      }

      @Override
      public File decryptImage(MultipartFile encryptedFile, String originalHash) throws Exception {
          log.info("Decrypting image: {}", encryptedFile.getOriginalFilename());

          // 암호화된 파일 데이터 추출
          byte[] combinedData = encryptedFile.getBytes();

          // salt와 iv 추출 (salt: 16 bytes, iv: 16 bytes)
          int saltLength = 16;
          int ivLength = 16;

          if (combinedData.length < saltLength + ivLength) {
              throw new IllegalArgumentException("Invalid encrypted file format. File too small.");
          }

          byte[] saltBytes = new byte[saltLength];
          byte[] ivBytes = new byte[ivLength];
          byte[] encryptedData = new byte[combinedData.length - saltLength - ivLength];

          System.arraycopy(combinedData, 0, saltBytes, 0, saltLength);
          System.arraycopy(combinedData, saltLength, ivBytes, 0, ivLength);
          System.arraycopy(
                  combinedData, saltLength + ivLength, encryptedData, 0, encryptedData.length);

          // Base64 인코딩
          String salt = Base64.encodeBase64String(saltBytes);
          String iv = Base64.encodeBase64String(ivBytes);

          // EncryptedImage 객체 생성 (서버 키 사용)
          EncryptedImage encryptedImage = new EncryptedImage();
          encryptedImage.encryptedData = encryptedData;
          encryptedImage.originalHash = originalHash;
          encryptedImage.salt = salt;
          encryptedImage.iv = iv;

          // 복호화 (서버 마스터 키 사용)
          byte[] decryptedData =
                  encryptionUtil.decryptImage(encryptedImage, encryptionUtil.getServerMasterKey());

          // 원본 해시가 제공된 경우 무결성 검증
          if (originalHash != null && !originalHash.isEmpty()) {
              // DigestUtils.sha256Hex를 사용하여 hex 형식으로 해시 계산
              String decryptedHash = DigestUtils.sha256Hex(decryptedData);
              if (!decryptedHash.equals(originalHash)) {
                  log.error(
                          "Hash mismatch! Original: {}, Decrypted: {}. Data integrity compromised.",
                          originalHash,
                          decryptedHash);
                  throw new SecurityException("Data integrity check failed. Hash mismatch.");
              }
              log.info("Hash verification successful. Data integrity confirmed.");
          }

          // 파일명에서 확장자 추출
          // 암호화된 파일명 형식: originalname.ext_encrypted.enc
          String originalFileName = encryptedFile.getOriginalFilename();
          String extension = "";
          if (originalFileName != null) {
              // "_encrypted.enc" 제거
              String baseName = originalFileName.replace("_encrypted.enc", "");
              // 마지막 점(.)의 위치로 확장자 추출
              if (baseName.contains(".")) {
                  extension = baseName.substring(baseName.lastIndexOf("."));
              }
          }

          // 확장자가 없으면 기본값 설정
          if (extension.isEmpty()) {
              extension = ".jpg"; // 이미지의 기본 확장자
          }

          // 임시 파일 생성 (확장자 포함)
          File tempFile = File.createTempFile("decrypted_", extension);
          tempFile.deleteOnExit();

          // 파일에 쓰기
          try (FileOutputStream fos = new FileOutputStream(tempFile)) {
              fos.write(decryptedData);
          }

          log.info(
                  "Decrypted to temp file: {}, size: {} bytes",
                  tempFile.getAbsolutePath(),
                  tempFile.length());

          return tempFile;
      }

      // ==================== PDF 2단계 암호화/복호화 ====================

      @Override
      public String uploadPdfStep1(MultipartFile pdfFile, String contractChatId, String password1)
              throws Exception {
          log.info(
                  "PDF encryption step 1 - file: {}, contractChatId: {}",
                  pdfFile.getOriginalFilename(),
                  contractChatId);

          // Redis에 파일과 첫 번째 패스워드 저장
          ContractKeyStatus status =
                  encryptionUtil.uploadContract(contractChatId, pdfFile, "owner", password1);

          log.info("Step 1 completed: {}", status.getMessage());
          return status.getMessage();
      }

      @Override
      public FileWithHashDto encryptPdfStep2(String contractChatId, String password2)
              throws Exception {
          log.info("PDF encryption step 2 - contractChatId: {}", contractChatId);

          // Redis에서 두 번째 패스워드 추가 및 암호화 수행
          ContractKeyStatus status =
                  encryptionUtil.uploadContract(contractChatId, null, "tenant", password2);

          if (status.getEncryptedPDF() == null) {
              throw new IllegalStateException("Encryption not ready. Status: " + status.getStatus());
          }

          EncryptedPDF encryptedPDF = status.getEncryptedPDF();

          // 원본 해시값 가져오기 (EncryptedPDF에는 이미 originalHash가 포함되어 있음)
          String originalHash = encryptedPDF.originalHash;

          // 파일명 생성
          String encryptedFilename = contractChatId + "_encrypted.2of3";
          if (encryptedPDF.originalFilename != null) {
              encryptedFilename =
                      encryptedPDF.originalFilename.replaceFirst("(\\.[^.]*)?$", "_encrypted.2of3");
          }

          // shares를 포함한 메타데이터 준비
          ObjectMapper objectMapper = new ObjectMapper();
          Map<String, Object> metadata = new HashMap<>();
          metadata.put("encryptedShares", encryptedPDF.encryptedShares); // shares 포함!
          metadata.put("iv", encryptedPDF.iv);
          metadata.put("originalHash", encryptedPDF.originalHash);
          metadata.put("threshold", encryptedPDF.threshold);
          metadata.put("totalShares", encryptedPDF.totalShares);
          metadata.put("originalFilename", encryptedPDF.originalFilename);

          String metadataJson = objectMapper.writeValueAsString(metadata);
          byte[] metadataBytes = metadataJson.getBytes(StandardCharsets.UTF_8);

          // 커스텀 파일 포맷 생성: [HEADER][VERSION][METADATA_LENGTH][METADATA][ENCRYPTED_DATA]
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          baos.write("2OF3PDF".getBytes(StandardCharsets.UTF_8)); // 7 bytes - 파일 식별자
          baos.write(ByteBuffer.allocate(4).putInt(1).array()); // 4 bytes - 버전
          baos.write(
                  ByteBuffer.allocate(4).putInt(metadataBytes.length).array()); // 4 bytes - 메타데이터 길이
          baos.write(metadataBytes); // 메타데이터 (shares 포함)
          baos.write(encryptedPDF.encryptedData); // 암호화된 PDF 데이터

          // 임시 파일 생성
          File tempFile = File.createTempFile(contractChatId + "_encrypted_", ".2of3");
          tempFile.deleteOnExit();

          // 파일에 쓰기
          try (FileOutputStream fos = new FileOutputStream(tempFile)) {
              fos.write(baos.toByteArray());
          }

          log.info(
                  "PDF encrypted to temp file with shares: {}, size: {} bytes, threshold: 2-of-3,"
                          + " hash: {}",
                  tempFile.getAbsolutePath(),
                  tempFile.length(),
                  originalHash);

          return FileWithHashDto.builder()
                  .file(tempFile)
                  .originalHash(originalHash)
                  .fileName(encryptedFilename)
                  .contentType("application/octet-stream") // 커스텀 포맷이므로 octet-stream
                  .build();
      }

      @Override
      public File decryptPdf(MultipartFile encryptedFile, String password, String originalHash)
              throws Exception {
          log.info("Decrypting PDF: {}", encryptedFile.getOriginalFilename());

          byte[] fileData = encryptedFile.getBytes();
          byte[] decryptedData;

          // 파일 유형 확인: 2-of-3 암호화인지 일반 패스워드 보호인지
          if (fileData.length >= 7) {
              String header = new String(fileData, 0, 7, StandardCharsets.UTF_8);
              if ("2OF3PDF".equals(header)) {
                  log.info("Detected 2-of-3 encrypted PDF file");
                  decryptedData = decrypt2Of3Pdf(fileData, password);
              } else {
                  log.info("Detected password-protected PDF file");
                  decryptedData = encryptionUtil.decryptPDFWithPassword(encryptedFile, password);
              }
          } else {
              // 파일이 너무 작으면 일반 PDF로 처리 시도
              decryptedData = encryptionUtil.decryptPDFWithPassword(encryptedFile, password);
          }

          // 원본 해시가 제공된 경우 무결성 검증
          if (originalHash != null && !originalHash.isEmpty()) {
              // DigestUtils.sha256Hex를 사용하여 hex 형식으로 해시 계산
              String decryptedHash = DigestUtils.sha256Hex(decryptedData);
              if (!decryptedHash.equals(originalHash)) {
                  log.error(
                          "Hash mismatch! Original: {}, Decrypted: {}. Data integrity compromised.",
                          originalHash,
                          decryptedHash);
                  throw new SecurityException("Data integrity check failed. Hash mismatch.");
              }
              log.info("Hash verification successful. Data integrity confirmed.");
          }

          // 파일명에서 확장자 추출
          String originalFileName = encryptedFile.getOriginalFilename();
          String extension = "";
          if (originalFileName != null) {
              // ".2of3", "_encrypted.enc", "_encrypted.pdf" 제거
              String baseName =
                      originalFileName
                              .replace(".2of3", "")
                              .replace("_encrypted.enc", "")
                              .replace("_encrypted.pdf", "");
              // 마지막 점(.)의 위치로 확장자 추출
              if (baseName.contains(".")) {
                  extension = baseName.substring(baseName.lastIndexOf("."));
              }
          }

          // 확장자가 없으면 기본값 설정
          if (extension.isEmpty()) {
              extension = ".pdf"; // PDF의 기본 확장자
          }

          // 임시 파일 생성 (확장자 포함)
          File tempFile = File.createTempFile("decrypted_", extension);
          tempFile.deleteOnExit();

          // 파일에 쓰기
          try (FileOutputStream fos = new FileOutputStream(tempFile)) {
              fos.write(decryptedData);
          }

          log.info(
                  "Decrypted to temp file: {}, size: {} bytes",
                  tempFile.getAbsolutePath(),
                  tempFile.length());

          return tempFile;
      }

      /** 2-of-3 암호화된 PDF 복호화 커스텀 파일 포맷에서 shares를 추출하여 복호화 */
      private byte[] decrypt2Of3Pdf(byte[] fileData, String password) throws Exception {
          log.info("Starting 2-of-3 PDF decryption");

          // 헤더 검증 (이미 위에서 확인했지만 다시 한번)
          String header = new String(fileData, 0, 7, StandardCharsets.UTF_8);
          if (!"2OF3PDF".equals(header)) {
              throw new IllegalArgumentException("Invalid 2-of-3 encrypted PDF file format");
          }

          // 버전 읽기
          int version = ByteBuffer.wrap(fileData, 7, 4).getInt();
          if (version != 1) {
              throw new IllegalArgumentException("Unsupported 2-of-3 file version: " + version);
          }

          // 메타데이터 길이 읽기
          int metadataLength = ByteBuffer.wrap(fileData, 11, 4).getInt();

          // 메타데이터 읽기
          String metadataJson = new String(fileData, 15, metadataLength, StandardCharsets.UTF_8);
          ObjectMapper objectMapper = new ObjectMapper();
          Map<String, Object> metadata = objectMapper.readValue(metadataJson, Map.class);

          log.info(
                  "Extracted metadata from 2-of-3 file: threshold={}, totalShares={}",
                  metadata.get("threshold"),
                  metadata.get("totalShares"));

          // EncryptedPDF 객체 재구성
          EncryptedPDF encryptedPDF = new EncryptedPDF();

          // 암호화된 데이터 추출
          int dataStartIndex = 15 + metadataLength;
          encryptedPDF.encryptedData = Arrays.copyOfRange(fileData, dataStartIndex, fileData.length);

          // 메타데이터에서 필드 복원
          encryptedPDF.iv = (String) metadata.get("iv");
          encryptedPDF.originalHash = (String) metadata.get("originalHash");
          encryptedPDF.threshold =
                  metadata.get("threshold") != null ? (Integer) metadata.get("threshold") : 2;
          encryptedPDF.totalShares =
                  metadata.get("totalShares") != null ? (Integer) metadata.get("totalShares") : 3;
          encryptedPDF.originalFilename = (String) metadata.get("originalFilename");

          // encryptedShares 복원 (Map<String, EncryptedShare>)
          Map<String, Map<String, Object>> sharesMap =
                  (Map<String, Map<String, Object>>) metadata.get("encryptedShares");

          if (sharesMap == null || sharesMap.isEmpty()) {
              throw new IllegalStateException("No encrypted shares found in 2-of-3 file");
          }

          encryptedPDF.encryptedShares = new HashMap<>();
          for (Map.Entry<String, Map<String, Object>> entry : sharesMap.entrySet()) {
              EncryptedShare encShare = new EncryptedShare();
              Map<String, Object> shareData = entry.getValue();
              encShare.data = (String) shareData.get("data"); // data 필드 (encryptedData 아님)
              encShare.iv = (String) shareData.get("iv");
              encryptedPDF.encryptedShares.put(entry.getKey(), encShare);
          }

          log.info("Reconstructed EncryptedPDF with {} shares", encryptedPDF.encryptedShares.size());

          // 2-of-3 복호화 수행
          // password는 owner 또는 tenant의 패스워드인지 모르므로 둘 다 시도
          // 서버 share는 자동으로 사용됨
          byte[] decryptedData = null;
          Exception lastException = null;

          // 먼저 owner password로 시도
          try {
              log.info("Attempting decryption with password as owner password");
              decryptedData = encryptionUtil.decryptPDF(encryptedPDF, password, null);
              log.info("Successfully decrypted with owner password");
          } catch (Exception e) {
              log.debug("Failed with owner password, trying tenant password: {}", e.getMessage());
              lastException = e;

              // owner로 실패하면 tenant password로 시도
              try {
                  log.info("Attempting decryption with password as tenant password");
                  decryptedData = encryptionUtil.decryptPDF(encryptedPDF, null, password);
                  log.info("Successfully decrypted with tenant password");
              } catch (Exception e2) {
                  log.error("Failed with both owner and tenant password attempts");
                  // Tag mismatch는 잘못된 패스워드를 의미
                  if (e.getMessage().contains("Tag mismatch")
                          || e2.getMessage().contains("Tag mismatch")) {
                      throw new IllegalArgumentException(
                              "잘못된 패스워드입니다. 올바른 임대인 또는 임차인 패스워드를 입력하세요.", e2);
                  }
                  // 다른 에러는 그대로 던짐
                  throw new IllegalArgumentException("복호화 실패: " + e2.getMessage(), e2);
              }
          }

          if (decryptedData == null) {
              throw new IllegalStateException("복호화에 실패했습니다.");
          }

          log.info("Successfully decrypted 2-of-3 PDF");
          return decryptedData;
      }

      // ==================== PDF 단순 비밀번호 ====================

      @Override
      public FileWithHashDto addPasswordToPdf(MultipartFile pdfFile, String password)
              throws Exception {
          log.info("Adding password to PDF: {}", pdfFile.getOriginalFilename());

          // 원본 파일의 해시값 계산
          byte[] originalData = pdfFile.getBytes();
          String originalHash = DigestUtils.sha256Hex(originalData);

          // PDF에 패스워드 설정 (편집 권한 제한 없음)
          byte[] protectedPdf = encryptionUtil.encryptPDFWithUserPassword(pdfFile, password);

          // 임시 파일 생성
          String originalName = pdfFile.getOriginalFilename();
          String baseName =
                  originalName != null ? originalName.replaceFirst("(\\.[^.]*)?$", "") : "protected";
          File tempFile = File.createTempFile(baseName + "_protected_", ".pdf");
          tempFile.deleteOnExit();

          // 파일에 쓰기
          try (FileOutputStream fos = new FileOutputStream(tempFile)) {
              fos.write(protectedPdf);
          }

          log.info(
                  "PDF password protected to temp file: {}, size: {} bytes, hash: {}",
                  tempFile.getAbsolutePath(),
                  tempFile.length(),
                  originalHash);

          return FileWithHashDto.builder()
                  .file(tempFile)
                  .originalHash(originalHash)
                  .fileName(baseName + "_protected.pdf")
                  .contentType("application/pdf")
                  .build();
      }
}
