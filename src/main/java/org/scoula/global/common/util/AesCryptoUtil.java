package org.scoula.global.common.util;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.annotation.PostConstruct;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.log4j.Log4j2;

/** AES 양방향 암호화 유틸리티 클래스 민감한 개인정보(주민등록번호, 전화번호 등)를 안전하게 암호화/복호화합니다. */
@Component
@Log4j2
public class AesCryptoUtil {

      private static final String ALGORITHM = "AES";
      private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
      private static final int IV_SIZE = 16; // 128 bits

      @Value("${crypto.aes.secret-key:#{null}}")
      private String secretKeyString;

      private SecretKey secretKey;
      private SecureRandom secureRandom;

      @PostConstruct
      public void init() {
          if (secretKeyString == null || secretKeyString.isEmpty()) {
              throw new IllegalStateException(
                      "AES 암호화 키가 설정되지 않았습니다. application.properties에 crypto.aes.secret-key를 설정하세요.");
          }

          try {
              // Base64로 인코딩된 키를 디코딩
              byte[] decodedKey = Base64.getDecoder().decode(secretKeyString);

              // 키 길이 검증 (AES-256은 32바이트 필요)
              if (decodedKey.length != 32) {
                  throw new IllegalStateException(
                          "AES 키는 256비트(32바이트)여야 합니다. 현재 길이: " + decodedKey.length);
              }

              this.secretKey = new SecretKeySpec(decodedKey, ALGORITHM);
              this.secureRandom = new SecureRandom();

              log.info("AES 암호화 유틸리티 초기화 완료");
          } catch (IllegalArgumentException e) {
              throw new IllegalStateException("잘못된 Base64 형식의 AES 키입니다.", e);
          }
      }

      /**
       * 문자열을 AES로 암호화합니다. IV(Initialization Vector)를 랜덤 생성하여 암호문 앞에 포함시킵니다.
       *
       * @param plainText 암호화할 평문
       * @return Base64로 인코딩된 암호문 (IV + 암호화된 데이터)
       */
      public String encrypt(String plainText) {
          if (plainText == null || plainText.isEmpty()) {
              return null;
          }

          try {
              // 랜덤 IV 생성
              byte[] iv = new byte[IV_SIZE];
              secureRandom.nextBytes(iv);
              IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

              // Cipher 초기화
              Cipher cipher = Cipher.getInstance(TRANSFORMATION);
              cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);

              // 암호화 수행
              byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

              // IV와 암호문을 결합
              byte[] combined = new byte[IV_SIZE + encryptedBytes.length];
              System.arraycopy(iv, 0, combined, 0, IV_SIZE);
              System.arraycopy(encryptedBytes, 0, combined, IV_SIZE, encryptedBytes.length);

              // Base64로 인코딩하여 반환
              return Base64.getEncoder().encodeToString(combined);

          } catch (NoSuchAlgorithmException
                  | NoSuchPaddingException
                  | InvalidKeyException
                  | InvalidAlgorithmParameterException
                  | IllegalBlockSizeException
                  | BadPaddingException e) {
              log.error("암호화 중 오류 발생", e);
              throw new RuntimeException("데이터 암호화에 실패했습니다.", e);
          }
      }

      /**
       * AES로 암호화된 문자열을 복호화합니다.
       *
       * @param encryptedText Base64로 인코딩된 암호문 (IV + 암호화된 데이터)
       * @return 복호화된 평문
       */
      public String decrypt(String encryptedText) {
          if (encryptedText == null || encryptedText.isEmpty()) {
              return null;
          }

          try {
              // Base64 디코딩
              byte[] combined = Base64.getDecoder().decode(encryptedText);

              // IV와 암호문 분리
              if (combined.length < IV_SIZE) {
                  throw new IllegalArgumentException("잘못된 암호화 데이터입니다.");
              }

              byte[] iv = new byte[IV_SIZE];
              byte[] encryptedBytes = new byte[combined.length - IV_SIZE];
              System.arraycopy(combined, 0, iv, 0, IV_SIZE);
              System.arraycopy(combined, IV_SIZE, encryptedBytes, 0, encryptedBytes.length);

              IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

              // Cipher 초기화
              Cipher cipher = Cipher.getInstance(TRANSFORMATION);
              cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);

              // 복호화 수행
              byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

              return new String(decryptedBytes, StandardCharsets.UTF_8);

          } catch (NoSuchAlgorithmException
                  | NoSuchPaddingException
                  | InvalidKeyException
                  | InvalidAlgorithmParameterException
                  | IllegalBlockSizeException
                  | BadPaddingException e) {
              log.error("복호화 중 오류 발생", e);
              throw new RuntimeException("데이터 복호화에 실패했습니다.", e);
          } catch (IllegalArgumentException e) {
              log.error("잘못된 암호화 데이터 형식", e);
              throw new RuntimeException("잘못된 암호화 데이터 형식입니다.", e);
          }
      }

      /**
       * 암호화된 데이터인지 확인합니다.
       *
       * @param text 확인할 문자열
       * @return 암호화된 데이터이면 true
       */
      public boolean isEncrypted(String text) {
          if (text == null || text.isEmpty()) {
              return false;
          }

          try {
              byte[] decoded = Base64.getDecoder().decode(text);
              return decoded.length > IV_SIZE;
          } catch (IllegalArgumentException e) {
              return false;
          }
      }

      /**
       * AES-256용 랜덤 키를 생성합니다. (초기 설정용)
       *
       * @return Base64로 인코딩된 256비트 키
       */
      public static String generateRandomKey() {
          SecureRandom random = new SecureRandom();
          byte[] key = new byte[32]; // 256 bits
          random.nextBytes(key);
          return Base64.getEncoder().encodeToString(key);
      }
}
