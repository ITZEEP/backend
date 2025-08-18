package org.scoula.global.common.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.annotation.PostConstruct;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.log4j.Log4j2;

/**
 * 이미지/바이너리 AES-GCM 암복호화 유틸. PNG/JPG 등 "포맷 무관" - 바이트 배열로 처리하므로 어떤 바이너리도 암/복호화 가능. 암호문 형식: Base64(
 * IV(12바이트) + CIPHERTEXT(암호문+TAG) ) - IV는 매 호출마다 SecureRandom으로 생성 - 키는 application.properties의
 * crypto.aes.secret-key(Base64 인코딩된 32바이트)를 사용
 */
@Component
@Log4j2
public class ImgAesCryptoUtil {

      private static final String ALGORITHM = "AES";
      private static final String TRANSFORMATION = "AES/GCM/NoPadding";
      private static final int IV_SIZE = 12; // 96 bits
      private static final int GCM_TAG_LENGTH = 128; // bits
      private static final int KEY_LEN = 32; // 256-bit

      @Value("${crypto.aes.secret-key:#{null}}")
      private String secretKeyString; // Base64로 인코딩된 32바이트 키

      private SecretKey secretKey;
      private SecureRandom secureRandom;

      @PostConstruct
      public void init() {
          if (secretKeyString == null || secretKeyString.isEmpty()) {
              throw new IllegalStateException(
                      "AES 키가 없습니다. crypto.aes.secret-key 를 설정하세요 (Base64-encoded 32 bytes).");
          }
          try {
              byte[] decodedKey = Base64.getDecoder().decode(secretKeyString);
              if (decodedKey.length != KEY_LEN) {
                  throw new IllegalStateException(
                          "AES-256 키 길이 오류: " + decodedKey.length + " bytes (32 필요)");
              }
              this.secretKey = new SecretKeySpec(decodedKey, ALGORITHM);
              this.secureRandom = new SecureRandom();
              log.info("ImgAesCryptoUtil initialized.");
          } catch (IllegalArgumentException e) {
              throw new IllegalStateException("잘못된 Base64 형식의 AES 키입니다.", e);
          }
      }

      // ======================== Public APIs ========================
      /** 바이트 배열 → AES-GCM → Base64(IV+암호문) */
      public String encryptBytesToBase64(byte[] plainBytes) {
          if (plainBytes == null || plainBytes.length == 0) return null;
          byte[] combined = encryptBytesInternal(plainBytes);
          return Base64.getEncoder().encodeToString(combined);
      }

      // ------------------------------
      /** Base64(IV+암호문) → 복호화 → 바이트 배열 */
      public byte[] decryptBase64ToBytes(String base64Cipher) {
          if (base64Cipher == null || base64Cipher.isEmpty()) return null;
          byte[] combined = Base64.getDecoder().decode(base64Cipher);
          return decryptBytesInternal(combined);
      }

      /** 파일(예: PNG/JPG/PDF 등 바이너리) → 암호화(Base64) */
      public String encryptFileToBase64(File file) throws IOException {
          if (file == null) return null;
          byte[] plain = java.nio.file.Files.readAllBytes(file.toPath());
          return encryptBytesToBase64(plain);
      }

      /** Base64(IV+암호문) → 복호화 → 파일로 저장 (확장자는 원본과 일치 권장) */
      public void decryptBase64ToFile(String base64Cipher, File outFile) throws IOException {
          if (base64Cipher == null || base64Cipher.isEmpty() || outFile == null) return;
          byte[] plain = decryptBase64ToBytes(base64Cipher);
          try (FileOutputStream fos = new FileOutputStream(outFile)) {
              fos.write(plain);
          }
      }

      // ---------------------------
      /** 업로드(MultipartFile) → 암호화(Base64). 이미지/서명 파일 등에 바로 사용 */
      public String encryptMultipartFileToBase64(MultipartFile multipartFile) throws IOException {
          if (multipartFile == null || multipartFile.isEmpty()) return null;
          return encryptBytesToBase64(multipartFile.getBytes());
      }

      /** InputStream → 암호화(Base64). S3/네트워크 스트림 등에서 유용 */
      public String encryptStreamToBase64(InputStream in) throws IOException {
          if (in == null) return null;
          byte[] plain = readAll(in);
          return encryptBytesToBase64(plain);
      }

      // ======================== Hash Utilities ========================
      /** 바이트 배열 → SHA-256 HEX 문자열 (무결성 검증용) */
      public static String sha256Hex(byte[] data) {
          if (data == null) return null;
          try {
              MessageDigest digest = MessageDigest.getInstance("SHA-256");
              byte[] hash = digest.digest(data);
              StringBuilder sb = new StringBuilder(hash.length * 2);
              for (byte b : hash) sb.append(String.format("%02x", b));
              return sb.toString();
          } catch (NoSuchAlgorithmException e) {
              throw new RuntimeException("SHA-256 사용 불가", e);
          }
      }

      /** 파일 → SHA-256 HEX */
      public static String sha256Hex(File file) throws IOException {
          if (file == null) return null;
          byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
          return sha256Hex(bytes);
      }

      /** 업로드 파일 → SHA-256 HEX */
      public static String sha256Hex(MultipartFile file) throws IOException {
          if (file == null || file.isEmpty()) return null;
          return sha256Hex(file.getBytes());
      }

      /** 바이트 배열 → SHA-256 Base64 */
      public static String sha256Base64(byte[] data) {
          if (data == null) return null;
          try {
              MessageDigest digest = MessageDigest.getInstance("SHA-256");
              byte[] hash = digest.digest(data);
              return Base64.getEncoder().encodeToString(hash);
          } catch (NoSuchAlgorithmException e) {
              throw new RuntimeException("SHA-256 사용 불가", e);
          }
      }

      // ======================== Internal Impl ========================
      private byte[] encryptBytesInternal(byte[] plainBytes) {
          try {
              byte[] iv = new byte[IV_SIZE];
              secureRandom.nextBytes(iv);
              GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
              Cipher cipher = Cipher.getInstance(TRANSFORMATION);
              cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);
              byte[] encrypted = cipher.doFinal(plainBytes);
              byte[] combined = new byte[IV_SIZE + encrypted.length];
              System.arraycopy(iv, 0, combined, 0, IV_SIZE);
              System.arraycopy(encrypted, 0, combined, IV_SIZE, encrypted.length);
              return combined;
          } catch (NoSuchAlgorithmException
                  | NoSuchPaddingException
                  | InvalidKeyException
                  | InvalidAlgorithmParameterException
                  | IllegalBlockSizeException
                  | BadPaddingException e) {
              log.error("이미지/바이너리 암호화 실패", e);
              throw new RuntimeException("이미지/바이너리 암호화 실패", e);
          }
      }

      private byte[] decryptBytesInternal(byte[] combined) {
          try {
              if (combined.length < IV_SIZE) throw new IllegalArgumentException("잘못된 암호화 데이터");
              byte[] iv = new byte[IV_SIZE];
              byte[] encryptedBytes = new byte[combined.length - IV_SIZE];
              System.arraycopy(combined, 0, iv, 0, IV_SIZE);
              System.arraycopy(combined, IV_SIZE, encryptedBytes, 0, encryptedBytes.length);
              GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
              Cipher cipher = Cipher.getInstance(TRANSFORMATION);
              cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
              return cipher.doFinal(encryptedBytes);
          } catch (NoSuchAlgorithmException
                  | NoSuchPaddingException
                  | InvalidKeyException
                  | InvalidAlgorithmParameterException
                  | IllegalBlockSizeException
                  | BadPaddingException e) {
              log.error("이미지/바이너리 복호화 실패", e);
              throw new RuntimeException("이미지/바이너리 복호화 실패", e);
          }
      }

      private static byte[] readAll(InputStream in) throws IOException {
          try (InputStream input = in;
                  ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
              byte[] buf = new byte[8192];
              int n;
              while ((n = input.read(buf)) != -1) {
                  baos.write(buf, 0, n);
              }
              return baos.toByteArray();
          }
      }

      /** 초기설정용 256-bit 키 생성기 (Base64 문자열) */
      public static String generateRandomKey() {
          SecureRandom random = new SecureRandom();
          byte[] key = new byte[KEY_LEN];
          random.nextBytes(key);
          return Base64.getEncoder().encodeToString(key);
      }
}
