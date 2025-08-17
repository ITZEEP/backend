package org.scoula.global.common.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.KeySpec;
import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.kernel.pdf.*;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

/** 통합 암호화 유틸리티 - PDF: Redis 기반 키 수집 + 2-of-3 봉투암호화, 이미지: AES 암호화 */
@Component
@Log4j2
public class EncryptionUtil {

      @Autowired private RedisTemplate<String, Object> redisTemplate;

      /** -- GETTER -- 서버 마스터 키 반환 */
      @Getter
      @Value("${crypto.aes.secret-key:itzip-server-master-key-2024-secure}")
      private String serverMasterKey;

      private final ObjectMapper objectMapper = new ObjectMapper();

      // Redis TTL (30분)
      private static final long REDIS_TTL_MINUTES = 30;

      // Redis 키 패턴
      private static final String REDIS_KEY_PATTERN = "contract:%s:keys";

      // AES 설정
      private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
      private static final String AES_ALGORITHM_SIMPLE = "AES/CBC/PKCS5Padding";
      private static final int AES_KEY_SIZE = 256;
      private static final int GCM_TAG_LENGTH = 128;
      private static final int GCM_IV_LENGTH = 12;
      private static final int IV_LENGTH = 16;
      private static final int SALT_LENGTH = 16;
      private static final int PBKDF2_ITERATIONS = 65536;

      // ==================== Redis 기반 계약 키 수집 시스템 ====================

      /** 계약서 업로드 및 첫 번째 키 등록 (내부 사용용 byte[] 버전) */
      private ContractKeyStatus uploadContractInternal(
              String fileId, byte[] pdfData, String keyType, String password) throws Exception {

          String redisKey = String.format(REDIS_KEY_PATTERN, fileId);

          try {
              log.info(
                      "Uploading contract with first key - fileId: {}, keyType: {}", fileId, keyType);

              ContractKeys contractKeys = new ContractKeys();
              contractKeys.setPdfData(Base64.encodeBase64String(pdfData));
              contractKeys.setCreatedTimestamp(new Date());

              if ("owner".equals(keyType)) {
                  contractKeys.setOwnerKey(password);
                  contractKeys.setStatus("WAITING_TENANT_KEY");
              } else if ("tenant".equals(keyType)) {
                  contractKeys.setTenantKey(password);
                  contractKeys.setStatus("WAITING_OWNER_KEY");
              } else {
                  throw new IllegalArgumentException("keyType must be 'owner' or 'tenant'");
              }

              redisTemplate
                      .opsForValue()
                      .set(redisKey, contractKeys, REDIS_TTL_MINUTES, TimeUnit.MINUTES);

              log.info("Contract uploaded successfully - waiting for second key");

              return ContractKeyStatus.builder()
                      .fileId(fileId)
                      .status(contractKeys.getStatus())
                      .message("First key registered. Waiting for second key.")
                      .build();

          } catch (Exception e) {
              log.error("Failed to upload contract: {}", e.getMessage(), e);
              redisTemplate.delete(redisKey);
              throw e;
          }
      }

      /** 두 번째 키 추가 및 자동 암호화 */
      public ContractEncryptionResult addSecondKey(String fileId, String keyType, String password)
              throws Exception {

          String redisKey = String.format(REDIS_KEY_PATTERN, fileId);

          try {
              log.info("Adding second key for contract - fileId: {}, keyType: {}", fileId, keyType);

              ContractKeys contractKeys = (ContractKeys) redisTemplate.opsForValue().get(redisKey);
              if (contractKeys == null) {
                  throw new IllegalArgumentException("Contract not found or expired: " + fileId);
              }

              if ("owner".equals(keyType)) {
                  if (contractKeys.getOwnerKey() != null) {
                      throw new IllegalArgumentException(
                              "Owner key already exists for contract: " + fileId);
                  }
                  contractKeys.setOwnerKey(password);
              } else if ("tenant".equals(keyType)) {
                  if (contractKeys.getTenantKey() != null) {
                      throw new IllegalArgumentException(
                              "Tenant key already exists for contract: " + fileId);
                  }
                  contractKeys.setTenantKey(password);
              } else {
                  throw new IllegalArgumentException("keyType must be 'owner' or 'tenant'");
              }

              if (contractKeys.getOwnerKey() == null || contractKeys.getTenantKey() == null) {
                  String waitingFor =
                          contractKeys.getOwnerKey() == null
                                  ? "WAITING_OWNER_KEY"
                                  : "WAITING_TENANT_KEY";
                  contractKeys.setStatus(waitingFor);
                  redisTemplate
                          .opsForValue()
                          .set(redisKey, contractKeys, REDIS_TTL_MINUTES, TimeUnit.MINUTES);

                  return ContractEncryptionResult.builder()
                          .fileId(fileId)
                          .status(waitingFor)
                          .message("Second key registered. Still waiting for one more key.")
                          .build();
              }

              log.info("Both keys ready - starting envelope encryption for fileId: {}", fileId);

              byte[] pdfData = Base64.decodeBase64(contractKeys.getPdfData());

              EncryptedPDF encryptedPDF =
                      encryptPDFInternal(
                              pdfData, contractKeys.getOwnerKey(), contractKeys.getTenantKey());

              log.info("Envelope encryption completed successfully for fileId: {}", fileId);

              redisTemplate.delete(redisKey);
              log.info("Contract keys deleted from Redis for security - fileId: {}", fileId);

              return ContractEncryptionResult.builder()
                      .fileId(fileId)
                      .status("ENCRYPTION_COMPLETED")
                      .message("Contract encrypted successfully with 2-of-3 envelope encryption.")
                      .encryptedPDF(encryptedPDF)
                      .build();

          } catch (Exception e) {
              log.error("Failed to add second key: {}", e.getMessage(), e);
              redisTemplate.delete(redisKey);
              throw e;
          }
      }

      // ==================== PDF 암호화 (2-of-3 Threshold) ====================

      /** PDF 파일 암호화 (내부 사용용 byte[] 버전) */
      private EncryptedPDF encryptPDFInternal(
              byte[] pdfData, String ownerPassword, String tenantPassword) throws Exception {
          // 1. 원본 해시값 계산
          String originalHash = DigestUtils.sha256Hex(pdfData);

          // 2. 데이터 암호화 키 생성
          SecretKey dataKey = generateAESKey();

          // 3. PDF 데이터 암호화
          byte[] iv = new byte[GCM_IV_LENGTH];
          new SecureRandom().nextBytes(iv);

          Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
          GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
          cipher.init(Cipher.ENCRYPT_MODE, dataKey, spec);
          byte[] encryptedData = cipher.doFinal(pdfData);

          // 4. 키를 3개로 분할 (단순화된 2-of-3 방식)
          byte[] keyBytes = dataKey.getEncoded();
          byte[] share1 = keyBytes.clone(); // server share
          byte[] share2 = keyBytes.clone(); // owner share
          byte[] share3 = keyBytes.clone(); // tenant share

          // 5. 각 Share를 개별 키로 암호화
          Map<String, EncryptedShare> encryptedShares = new HashMap<>();

          // 서버 키로 Share 1 암호화
          log.debug("Encrypting server share with master key");
          SecretKey serverKey = deriveKeyFromPassword(serverMasterKey, "server-salt");
          encryptedShares.put("server", encryptShare(share1, serverKey));

          // 임대인 키로 Share 2 암호화
          log.debug("Encrypting owner share with password: {}", ownerPassword);
          SecretKey ownerKey = deriveKeyFromPassword(ownerPassword, "owner-salt");
          encryptedShares.put("owner", encryptShare(share2, ownerKey));

          // 임차인 키로 Share 3 암호화
          if (tenantPassword != null && !tenantPassword.isEmpty()) {
              log.debug("Encrypting tenant share with password: {}", tenantPassword);
              SecretKey tenantKey = deriveKeyFromPassword(tenantPassword, "tenant-salt");
              encryptedShares.put("tenant", encryptShare(share3, tenantKey));
          }

          // 6. 결과 반환
          EncryptedPDF result = new EncryptedPDF();
          result.encryptedData = encryptedData;
          result.iv = Base64.encodeBase64String(iv);
          result.originalHash = originalHash;
          result.encryptedShares = encryptedShares;
          result.threshold = 2;
          result.totalShares = 3;
          result.encryptionTimestamp = new Date();

          return result;
      }

      /**
       * PDF 파일 복호화 (EncryptedPDF 객체 사용)
       *
       * @param encryptedPDF 암호화된 PDF 정보
       * @param ownerPassword 임대인 패스워드 (선택)
       * @param tenantPassword 임차인 패스워드 (선택)
       * @return 복호화된 PDF 데이터
       */
      public byte[] decryptPDF(EncryptedPDF encryptedPDF, String ownerPassword, String tenantPassword)
              throws Exception {
          // 1. 사용 가능한 Share 복호화
          List<byte[]> decryptedShares = new ArrayList<>();

          // 서버 Share는 항상 복호화
          if (encryptedPDF.encryptedShares.containsKey("server")) {
              try {
                  log.debug("Decrypting server share with master key");
                  SecretKey serverKey = deriveKeyFromPassword(serverMasterKey, "server-salt");
                  byte[] share = decryptShare(encryptedPDF.encryptedShares.get("server"), serverKey);
                  decryptedShares.add(share);
                  log.debug("Server share decrypted successfully");
              } catch (Exception e) {
                  log.error("Failed to decrypt server share: {}", e.getMessage());
              }
          }

          // 임대인 Share 복호화
          if (ownerPassword != null && encryptedPDF.encryptedShares.containsKey("owner")) {
              try {
                  log.debug("Decrypting owner share with password: {}", ownerPassword);
                  SecretKey ownerKey = deriveKeyFromPassword(ownerPassword, "owner-salt");
                  byte[] share = decryptShare(encryptedPDF.encryptedShares.get("owner"), ownerKey);
                  decryptedShares.add(share);
                  log.debug("Owner share decrypted successfully");
              } catch (Exception e) {
                  log.error("Failed to decrypt owner share: {}", e.getMessage());
              }
          }

          // 임차인 Share 복호화
          if (tenantPassword != null && encryptedPDF.encryptedShares.containsKey("tenant")) {
              try {
                  log.debug("Decrypting tenant share with password: {}", tenantPassword);
                  SecretKey tenantKey = deriveKeyFromPassword(tenantPassword, "tenant-salt");
                  byte[] share = decryptShare(encryptedPDF.encryptedShares.get("tenant"), tenantKey);
                  decryptedShares.add(share);
                  log.debug("Tenant share decrypted successfully");
              } catch (Exception e) {
                  log.error("Failed to decrypt tenant share: {}", e.getMessage());
              }
          }

          // 2. 최소 2개의 Share 필요
          if (decryptedShares.size() < 2) {
              throw new IllegalArgumentException(
                      "복호화 실패: 최소 2개의 키가 필요합니다. (현재: " + decryptedShares.size() + "개)");
          }

          // 3. 키 복원 - 단순화된 방식: 모든 share가 동일한 키를 가지고 있음
          // 첫 번째 복호화된 share를 그대로 키로 사용
          byte[] keyBytes = decryptedShares.get(0);

          System.out.println("DEBUG: Recovering key from " + decryptedShares.size() + " shares");
          System.out.println("DEBUG: Using first share as key (length: " + keyBytes.length + ")");

          SecretKey dataKey = new SecretKeySpec(keyBytes, "AES");

          // 4. 데이터 복호화
          byte[] iv = Base64.decodeBase64(encryptedPDF.iv);
          Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
          GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
          cipher.init(Cipher.DECRYPT_MODE, dataKey, spec);
          byte[] decryptedData = cipher.doFinal(encryptedPDF.encryptedData);

          // 5. 해시 검증
          String decryptedHash = DigestUtils.sha256Hex(decryptedData);
          if (!decryptedHash.equals(encryptedPDF.originalHash)) {
              throw new SecurityException("해시값 불일치! 데이터가 손상되었을 수 있습니다.");
          }

          return decryptedData;
      }

      // ==================== PDF 직접 암호화 (iText) ====================

      /** PDF 파일에 직접 암호를 설정하여 암호화 (내부 사용용) */
      private byte[] encryptPDFWithPasswordInternal(
              byte[] pdfData, String userPassword, String ownerPassword) throws Exception {
          log.info("Encrypting PDF with user password protection");

          try {
              // 입력 PDF 읽기
              PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfData));

              // 출력 스트림 준비
              ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

              // 암호화 설정
              WriterProperties writerProperties = new WriterProperties();

              // 암호 설정 - 사용자 암호와 소유자 암호
              writerProperties.setStandardEncryption(
                      userPassword != null ? userPassword.getBytes() : null,
                      ownerPassword != null ? ownerPassword.getBytes() : userPassword.getBytes(),
                      EncryptionConstants.ALLOW_PRINTING | EncryptionConstants.ALLOW_COPY,
                      EncryptionConstants.ENCRYPTION_AES_128);

              PdfWriter writer = new PdfWriter(outputStream, writerProperties);
              PdfDocument pdfDoc = new PdfDocument(reader, writer);

              // PDF 문서 닫기 (자동으로 암호화됨)
              pdfDoc.close();
              reader.close();

              byte[] encryptedPdf = outputStream.toByteArray();
              log.info(
                      "PDF encrypted successfully with password protection, size: {} bytes",
                      encryptedPdf.length);

              return encryptedPdf;

          } catch (Exception e) {
              log.error("Failed to encrypt PDF with password: {}", e.getMessage(), e);
              throw new Exception("PDF encryption failed: " + e.getMessage(), e);
          }
      }

      /** 암호화된 PDF 복호화 (내부 사용용) */
      private byte[] decryptPDFWithPasswordInternal(byte[] encryptedPdfData, String password)
              throws Exception {
          log.info("Attempting to decrypt PDF with provided password");

          // PDF 헤더 확인 (첫 4바이트가 %PDF 인지)
          if (encryptedPdfData == null || encryptedPdfData.length < 4) {
              throw new IllegalArgumentException("Invalid PDF data: file is empty or too small");
          }

          String header = new String(encryptedPdfData, 0, Math.min(4, encryptedPdfData.length));
          if (!header.startsWith("%PDF")) {
              log.error("File does not appear to be a PDF. First 4 bytes: {}", header);
              throw new IllegalArgumentException(
                      "File is not a valid PDF document. Please ensure you're uploading a PDF file.");
          }

          try {
              // 암호를 사용하여 PDF 읽기
              ReaderProperties readerProperties = new ReaderProperties();
              readerProperties.setPassword(password.getBytes());

              PdfReader reader =
                      new PdfReader(new ByteArrayInputStream(encryptedPdfData), readerProperties);

              // 출력 스트림 준비 (암호화 해제된 PDF)
              ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
              PdfWriter writer = new PdfWriter(outputStream);
              PdfDocument pdfDoc = new PdfDocument(reader, writer);

              // PDF 문서 닫기 (암호화 해제됨)
              pdfDoc.close();
              reader.close();

              byte[] decryptedPdf = outputStream.toByteArray();
              log.info("PDF decrypted successfully, size: {} bytes", decryptedPdf.length);

              return decryptedPdf;

          } catch (Exception e) {
              log.error("Failed to decrypt PDF with password: {}", e.getMessage(), e);
              throw new Exception(
                      "PDF decryption failed - invalid password or corrupted file: " + e.getMessage(),
                      e);
          }
      }

      // ==================== MultipartFile 처리 메서드 ====================

      /** PDF 파일 암호화 (메인 메서드 - MultipartFile 사용) */
      public EncryptedPDF encryptPDF(
              MultipartFile pdfFile, String ownerPassword, String tenantPassword) throws Exception {
          log.info("Encrypting PDF from MultipartFile: {}", pdfFile.getOriginalFilename());

          // 파일 유효성 검증
          validateMultipartFile(pdfFile, "pdf");

          // MultipartFile을 byte array로 변환
          byte[] pdfData = pdfFile.getBytes();

          // 내부 암호화 메서드 호출
          EncryptedPDF result = encryptPDFInternal(pdfData, ownerPassword, tenantPassword);

          // 파일 정보 추가
          result.originalFilename = pdfFile.getOriginalFilename();
          result.contentType = pdfFile.getContentType();

          return result;
      }

      /** PDF 파일에 직접 암호 설정 (메인 메서드 - MultipartFile 사용) */
      public byte[] encryptPDFWithPassword(
              MultipartFile pdfFile, String userPassword, String ownerPassword) throws Exception {
          log.info(
                  "Encrypting PDF with password from MultipartFile: {}",
                  pdfFile.getOriginalFilename());

          // 파일 유효성 검증
          validateMultipartFile(pdfFile, "pdf");

          // MultipartFile을 byte array로 변환
          byte[] pdfData = pdfFile.getBytes();

          // PDF 암호화 수행
          return encryptPDFWithPasswordInternal(pdfData, userPassword, ownerPassword);
      }

      /** 암호화된 PDF 복호화 (메인 메서드 - MultipartFile 사용) */
      public byte[] decryptPDFWithPassword(MultipartFile encryptedPdfFile, String password)
              throws Exception {
          log.info(
                  "Decrypting PDF with password from MultipartFile: {}",
                  encryptedPdfFile.getOriginalFilename());

          // 파일 유효성 검증
          validateMultipartFile(encryptedPdfFile, "pdf");

          // MultipartFile을 byte array로 변환
          byte[] encryptedPdfData = encryptedPdfFile.getBytes();

          // PDF 복호화 수행
          return decryptPDFWithPasswordInternal(encryptedPdfData, password);
      }

      /** 이미지 MultipartFile 암호화 (서버 마스터 키 사용) */
      public EncryptedImage encryptImageFromMultipartFile(MultipartFile imageFile) throws Exception {
          log.info("Encrypting image from MultipartFile: {}", imageFile.getOriginalFilename());

          // MultipartFile을 byte array로 변환
          byte[] imageData = imageFile.getBytes();

          // 원본 해시값
          String originalHash = DigestUtils.sha256Hex(imageData);

          // Salt와 IV 생성
          byte[] salt = new byte[SALT_LENGTH];
          byte[] iv = new byte[IV_LENGTH];
          new SecureRandom().nextBytes(salt);
          new SecureRandom().nextBytes(iv);

          // 서버 마스터 키에서 키 도출
          SecretKey key = deriveKeyFromPassword(serverMasterKey, Base64.encodeBase64String(salt));

          // 암호화
          Cipher cipher = Cipher.getInstance(AES_ALGORITHM_SIMPLE);
          cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
          byte[] encryptedData = cipher.doFinal(imageData);

          // 결과 반환
          EncryptedImage result = new EncryptedImage();
          result.encryptedData = encryptedData;
          result.iv = Base64.encodeBase64String(iv);
          result.salt = Base64.encodeBase64String(salt);
          result.originalHash = originalHash;
          result.algorithm = AES_ALGORITHM_SIMPLE;
          result.originalFilename = imageFile.getOriginalFilename();
          result.contentType = imageFile.getContentType();

          log.info(
                  "Image encrypted successfully from MultipartFile, encrypted size: {} bytes",
                  encryptedData.length);

          return result;
      }

      /** PDF 파일에 사용자 암호만 설정 (메인 메서드 - MultipartFile 사용) */
      public byte[] encryptPDFWithUserPassword(MultipartFile pdfFile, String password)
              throws Exception {
          log.info(
                  "Encrypting PDF with user password from MultipartFile: {}",
                  pdfFile.getOriginalFilename());

          // 파일 유효성 검증
          validateMultipartFile(pdfFile, "pdf");

          // MultipartFile을 byte array로 변환
          byte[] pdfData = pdfFile.getBytes();

          // PDF 암호화 수행
          return encryptPDFWithPasswordInternal(pdfData, password, password);
      }

      /** 데이터의 SHA-256 해시 계산 */
      public String calculateHash(byte[] data) throws NoSuchAlgorithmException {
          MessageDigest digest = MessageDigest.getInstance("SHA-256");
          byte[] hash = digest.digest(data);
          return Base64.encodeBase64String(hash);
      }

      /** 계약서 업로드 (메인 메서드 - MultipartFile 사용) */
      public ContractKeyStatus uploadContract(
              String fileId, MultipartFile pdfFile, String keyType, String password)
              throws Exception {

          // pdfFile이 null인 경우 - Step1: 첫 번째 키만 저장
          if (pdfFile == null && "owner".equals(keyType)) {
              log.info("Step 1: Saving first key only - fileId: {}, keyType: {}", fileId, keyType);
              return saveFirstKeyOnly(fileId, keyType, password);
          }

          // pdfFile이 있는 경우 - Step2: PDF 파일과 두 번째 키 저장
          if (pdfFile != null && "tenant".equals(keyType)) {
              log.info(
                      "Step 2: Saving PDF file and second key - fileId: {}, keyType: {}",
                      fileId,
                      keyType);
              return savePdfAndSecondKey(fileId, pdfFile, keyType, password);
          }

          // 기존 로직 (호환성 유지)
          if (pdfFile == null) {
              log.info("Adding second key - fileId: {}, keyType: {}", fileId, keyType);
              ContractEncryptionResult result = addSecondKey(fileId, keyType, password);
              return ContractKeyStatus.builder()
                      .fileId(fileId)
                      .status(result.getStatus())
                      .message(result.getMessage())
                      .encryptedPDF(result.getEncryptedPDF())
                      .hasOwnerKey(true)
                      .hasTenantKey(true)
                      .build();
          }

          // 첫 번째 키 추가 (파일 업로드) - 기존 로직
          log.info("Uploading contract from MultipartFile: {}", pdfFile.getOriginalFilename());
          validateMultipartFile(pdfFile, "pdf");
          byte[] pdfData = pdfFile.getBytes();
          return uploadContractInternal(fileId, pdfData, keyType, password);
      }

      /** Step1: 첫 번째 패스워드만 저장 (PDF 파일 없이) */
      private ContractKeyStatus saveFirstKeyOnly(String fileId, String keyType, String password)
              throws Exception {
          String redisKey = String.format(REDIS_KEY_PATTERN, fileId);

          log.info("Saving first key only - fileId: {}, keyType: {}", fileId, keyType);

          ContractKeys contractKeys = new ContractKeys();
          contractKeys.setCreatedTimestamp(new Date());
          contractKeys.setOwnerKey(password);
          contractKeys.setStatus("WAITING_PDF_AND_TENANT_KEY");

          // Redis에 저장
          redisTemplate.opsForValue().set(redisKey, contractKeys, 60L, TimeUnit.MINUTES);

          return ContractKeyStatus.builder()
                  .fileId(fileId)
                  .status("WAITING_PDF_AND_TENANT_KEY")
                  .message("First key saved. Waiting for PDF file and second key.")
                  .hasOwnerKey(true)
                  .hasTenantKey(false)
                  .build();
      }

      /** Step2: PDF 파일과 두 번째 패스워드 저장 및 암호화 수행 */
      private ContractKeyStatus savePdfAndSecondKey(
              String fileId, MultipartFile pdfFile, String keyType, String password)
              throws Exception {
          String redisKey = String.format(REDIS_KEY_PATTERN, fileId);

          log.info("Saving PDF and second key - fileId: {}, keyType: {}", fileId, keyType);

          // Redis에서 기존 데이터 조회
          ContractKeys existingKeys = (ContractKeys) redisTemplate.opsForValue().get(redisKey);
          if (existingKeys == null) {
              throw new IllegalArgumentException(
                      "Contract not found. Please complete Step 1 first: " + fileId);
          }

          // 파일 유효성 검증
          validateMultipartFile(pdfFile, "pdf");
          byte[] pdfData = pdfFile.getBytes();

          // PDF 데이터와 두 번째 키 추가
          existingKeys.setPdfData(Base64.encodeBase64String(pdfData));
          existingKeys.setTenantKey(password);
          existingKeys.setStatus("READY_FOR_ENCRYPTION");

          // Redis 업데이트
          redisTemplate.opsForValue().set(redisKey, existingKeys, 60L, TimeUnit.MINUTES);

          // 두 키가 모두 있으므로 암호화 수행
          if (existingKeys.getOwnerKey() != null && existingKeys.getTenantKey() != null) {
              log.info("Both keys present. Performing 2-of-3 encryption");

              EncryptedPDF encryptedPDF =
                      encryptPDFInternal(
                              pdfData, existingKeys.getOwnerKey(), existingKeys.getTenantKey());

              // Redis에서 삭제 (암호화 완료)
              redisTemplate.delete(redisKey);

              return ContractKeyStatus.builder()
                      .fileId(fileId)
                      .status("ENCRYPTION_COMPLETE")
                      .message("PDF encrypted successfully with 2-of-3 scheme")
                      .encryptedPDF(encryptedPDF)
                      .hasOwnerKey(true)
                      .hasTenantKey(true)
                      .build();
          }

          return ContractKeyStatus.builder()
                  .fileId(fileId)
                  .status("ERROR")
                  .message("Unexpected state")
                  .hasOwnerKey(true)
                  .hasTenantKey(true)
                  .build();
      }

      /** 두 번째 키 추가 및 자동 암호화 (결과에 파일 정보 포함) */
      public ContractEncryptionResult addSecondKeyWithFileInfo(
              String fileId, String keyType, String password, String originalFilename)
              throws Exception {

          // 기존 메서드 호출
          ContractEncryptionResult result = addSecondKey(fileId, keyType, password);

          // 암호화가 완료된 경우 파일 정보 추가
          if (result.getEncryptedPDF() != null) {
              result.getEncryptedPDF().originalFilename = originalFilename;
              result.getEncryptedPDF().contentType = "application/pdf";
          }

          return result;
      }

      /** MultipartFile 유효성 검증 헬퍼 메서드 */
      private void validateMultipartFile(MultipartFile file, String expectedType)
              throws IllegalArgumentException {
          if (file == null || file.isEmpty()) {
              throw new IllegalArgumentException("File is empty or null");
          }

          String contentType = file.getContentType();
          if (contentType == null) {
              throw new IllegalArgumentException("File content type is null");
          }

          if (expectedType.equals("pdf") && !contentType.toLowerCase().contains("pdf")) {
              throw new IllegalArgumentException("File is not a PDF. Content type: " + contentType);
          }

          if (expectedType.equals("image") && !contentType.toLowerCase().startsWith("image/")) {
              throw new IllegalArgumentException(
                      "File is not an image. Content type: " + contentType);
          }
      }

      // ==================== 이미지 암호화 (AES) ====================

      /** 이미지 파일 암호화 (내부 사용용 byte[] 버전) */
      private EncryptedImage encryptImageInternal(byte[] imageData, String password)
              throws Exception {
          // 원본 해시값
          String originalHash = DigestUtils.sha256Hex(imageData);

          // Salt와 IV 생성
          byte[] salt = new byte[SALT_LENGTH];
          byte[] iv = new byte[IV_LENGTH];
          new SecureRandom().nextBytes(salt);
          new SecureRandom().nextBytes(iv);

          // 패스워드에서 키 도출
          SecretKey key = deriveKeyFromPassword(password, Base64.encodeBase64String(salt));

          // 암호화
          Cipher cipher = Cipher.getInstance(AES_ALGORITHM_SIMPLE);
          cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
          byte[] encryptedData = cipher.doFinal(imageData);

          // 결과 반환
          EncryptedImage result = new EncryptedImage();
          result.encryptedData = encryptedData;
          result.iv = Base64.encodeBase64String(iv);
          result.salt = Base64.encodeBase64String(salt);
          result.originalHash = originalHash;
          result.algorithm = AES_ALGORITHM_SIMPLE;

          return result;
      }

      /** 이미지 파일 복호화 (EncryptedImage 객체 사용) */
      public byte[] decryptImage(EncryptedImage encryptedImage, String password) throws Exception {
          // Salt와 IV 디코딩
          byte[] salt = Base64.decodeBase64(encryptedImage.salt);
          byte[] iv = Base64.decodeBase64(encryptedImage.iv);

          // 패스워드에서 키 도출
          SecretKey key = deriveKeyFromPassword(password, Base64.encodeBase64String(salt));

          // 알고리즘 설정 (null이면 기본값 사용)
          String algorithm = encryptedImage.algorithm;
          if (algorithm == null || algorithm.isEmpty()) {
              algorithm = "AES/CBC/PKCS5Padding";
          }

          // 복호화
          Cipher cipher = Cipher.getInstance(algorithm);
          cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
          byte[] decryptedData = cipher.doFinal(encryptedImage.encryptedData);

          // 해시 검증
          String decryptedHash = DigestUtils.sha256Hex(decryptedData);
          if (!decryptedHash.equals(encryptedImage.originalHash)) {
              throw new SecurityException("해시값 불일치! 잘못된 패스워드이거나 데이터가 손상되었습니다.");
          }

          return decryptedData;
      }

      // ==================== 헬퍼 메서드 ====================

      private SecretKey generateAESKey() throws NoSuchAlgorithmException {
          KeyGenerator keyGen = KeyGenerator.getInstance("AES");
          keyGen.init(AES_KEY_SIZE);
          return keyGen.generateKey();
      }

      private SecretKey deriveKeyFromPassword(String password, String salt) throws Exception {
          System.out.println(
                  "DEBUG: Deriving key from password - Password: " + password + ", Salt: " + salt);

          SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
          KeySpec spec =
                  new PBEKeySpec(
                          password.toCharArray(),
                          salt.getBytes(StandardCharsets.UTF_8),
                          PBKDF2_ITERATIONS,
                          AES_KEY_SIZE);
          SecretKey tmp = factory.generateSecret(spec);
          SecretKey key = new SecretKeySpec(tmp.getEncoded(), "AES");

          System.out.println(
                  "DEBUG: Key derived - Algorithm: "
                          + key.getAlgorithm()
                          + ", Format: "
                          + key.getFormat()
                          + ", Encoded length: "
                          + key.getEncoded().length);

          return key;
      }

      private EncryptedShare encryptShare(byte[] share, SecretKey key) throws Exception {
          byte[] iv = new byte[GCM_IV_LENGTH];
          new SecureRandom().nextBytes(iv);

          // Static 메서드에서는 static logger 사용
          System.out.println(
                  "DEBUG: Encrypting share - Share length: "
                          + share.length
                          + ", IV length: "
                          + iv.length);

          Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
          GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
          cipher.init(Cipher.ENCRYPT_MODE, key, spec);
          byte[] encryptedData = cipher.doFinal(share);

          System.out.println(
                  "DEBUG: Share encrypted - Encrypted data length: " + encryptedData.length);

          EncryptedShare result = new EncryptedShare();
          result.data = Base64.encodeBase64String(encryptedData);
          result.iv = Base64.encodeBase64String(iv);
          return result;
      }

      private byte[] decryptShare(EncryptedShare encShare, SecretKey key) throws Exception {
          byte[] encryptedData = Base64.decodeBase64(encShare.data);
          byte[] iv = Base64.decodeBase64(encShare.iv);

          System.out.println(
                  "DEBUG: Decrypting share - Encrypted data length: "
                          + encryptedData.length
                          + ", IV length: "
                          + iv.length);
          System.out.println(
                  "DEBUG: Key algorithm: " + key.getAlgorithm() + ", Key format: " + key.getFormat());

          Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
          GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
          cipher.init(Cipher.DECRYPT_MODE, key, spec);

          try {
              byte[] decryptedData = cipher.doFinal(encryptedData);
              System.out.println(
                      "DEBUG: Share decrypted successfully, length: " + decryptedData.length);
              return decryptedData;
          } catch (Exception e) {
              System.err.println("ERROR: Failed to decrypt share: " + e.getMessage());
              System.err.println(
                      "ERROR: Encrypted data (first 50 bytes): "
                              + (encryptedData.length > 50
                                      ? Arrays.toString(Arrays.copyOfRange(encryptedData, 0, 50))
                                      : Arrays.toString(encryptedData)));
              throw new IllegalArgumentException("Tag mismatch - invalid key or corrupted data", e);
          }
      }

      // ==================== 내부 클래스 ====================

      /** Redis에 저장될 계약 키 정보 */
      public static class ContractKeys {
          private String ownerKey;
          private String tenantKey;
          private String pdfData; // Base64 encoded
          private String status;
          private Date createdTimestamp;

          // Getters and Setters
          public String getOwnerKey() {
              return ownerKey;
          }

          public void setOwnerKey(String ownerKey) {
              this.ownerKey = ownerKey;
          }

          public String getTenantKey() {
              return tenantKey;
          }

          public void setTenantKey(String tenantKey) {
              this.tenantKey = tenantKey;
          }

          public String getPdfData() {
              return pdfData;
          }

          public void setPdfData(String pdfData) {
              this.pdfData = pdfData;
          }

          public String getStatus() {
              return status;
          }

          public void setStatus(String status) {
              this.status = status;
          }

          public Date getCreatedTimestamp() {
              return createdTimestamp;
          }

          public void setCreatedTimestamp(Date createdTimestamp) {
              this.createdTimestamp = createdTimestamp;
          }
      }

      /** 계약 상태 응답 */
      public static class ContractKeyStatus {
          private String fileId;
          private String status;
          private String message;
          private Date createdTimestamp;
          private boolean hasOwnerKey;
          private boolean hasTenantKey;
          private EncryptedPDF encryptedPDF;

          public static ContractKeyStatusBuilder builder() {
              return new ContractKeyStatusBuilder();
          }

          public static class ContractKeyStatusBuilder {
              private ContractKeyStatus status = new ContractKeyStatus();

              public ContractKeyStatusBuilder fileId(String fileId) {
                  status.fileId = fileId;
                  return this;
              }

              public ContractKeyStatusBuilder status(String statusValue) {
                  status.status = statusValue;
                  return this;
              }

              public ContractKeyStatusBuilder message(String message) {
                  status.message = message;
                  return this;
              }

              public ContractKeyStatusBuilder createdTimestamp(Date timestamp) {
                  status.createdTimestamp = timestamp;
                  return this;
              }

              public ContractKeyStatusBuilder hasOwnerKey(boolean hasOwnerKey) {
                  status.hasOwnerKey = hasOwnerKey;
                  return this;
              }

              public ContractKeyStatusBuilder hasTenantKey(boolean hasTenantKey) {
                  status.hasTenantKey = hasTenantKey;
                  return this;
              }

              public ContractKeyStatusBuilder encryptedPDF(EncryptedPDF encryptedPDF) {
                  status.encryptedPDF = encryptedPDF;
                  return this;
              }

              public ContractKeyStatus build() {
                  return status;
              }
          }

          // Getters
          public String getFileId() {
              return fileId;
          }

          public String getStatus() {
              return status;
          }

          public String getMessage() {
              return message;
          }

          public Date getCreatedTimestamp() {
              return createdTimestamp;
          }

          public boolean isHasOwnerKey() {
              return hasOwnerKey;
          }

          public boolean isHasTenantKey() {
              return hasTenantKey;
          }

          public EncryptedPDF getEncryptedPDF() {
              return encryptedPDF;
          }
      }

      /** 계약 암호화 결과 */
      public static class ContractEncryptionResult {
          private String fileId;
          private String status;
          private String message;
          private EncryptedPDF encryptedPDF;

          public static ContractEncryptionResultBuilder builder() {
              return new ContractEncryptionResultBuilder();
          }

          public static class ContractEncryptionResultBuilder {
              private ContractEncryptionResult result = new ContractEncryptionResult();

              public ContractEncryptionResultBuilder fileId(String fileId) {
                  result.fileId = fileId;
                  return this;
              }

              public ContractEncryptionResultBuilder status(String status) {
                  result.status = status;
                  return this;
              }

              public ContractEncryptionResultBuilder message(String message) {
                  result.message = message;
                  return this;
              }

              public ContractEncryptionResultBuilder encryptedPDF(EncryptedPDF encryptedPDF) {
                  result.encryptedPDF = encryptedPDF;
                  return this;
              }

              public ContractEncryptionResult build() {
                  return result;
              }
          }

          // Getters
          public String getFileId() {
              return fileId;
          }

          public String getStatus() {
              return status;
          }

          public String getMessage() {
              return message;
          }

          public EncryptedPDF getEncryptedPDF() {
              return encryptedPDF;
          }
      }

      /** 암호화된 PDF 정보 */
      public static class EncryptedPDF {
          public byte[] encryptedData;
          public String iv;
          public String originalHash;
          public Map<String, EncryptedShare> encryptedShares;
          public int threshold;
          public int totalShares;
          public Date encryptionTimestamp;
          public String originalFilename; // 원본 파일명
          public String contentType; // MIME 타입
      }

      /** 암호화된 이미지 정보 */
      public static class EncryptedImage {
          public byte[] encryptedData;
          public String iv;
          public String salt;
          public String originalHash;
          public String algorithm;
          public String originalFilename; // 원본 파일명
          public String contentType; // MIME 타입
      }

      /** 암호화된 Share */
      public static class EncryptedShare {
          public String data;
          public String iv;
      }

      /**
       * Redis에서 계약 ID에 대한 키 존재 여부 확인
       *
       * @param contractChatId 계약 채팅 ID
       * @return 키 존재 여부 (true: 키가 존재함, false: 키가 없음)
       */
      public boolean hasKey(String contractChatId) {
          if (contractChatId == null || contractChatId.trim().isEmpty()) {
              return false;
          }

          String redisKey = String.format(REDIS_KEY_PATTERN, contractChatId);
          return Boolean.TRUE.equals(redisTemplate.hasKey(redisKey));
      }
}
