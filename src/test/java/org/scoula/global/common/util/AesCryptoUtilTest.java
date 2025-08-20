package org.scoula.global.common.util;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("AesCryptoUtil 테스트")
class AesCryptoUtilTest {

      private AesCryptoUtil aesCryptoUtil;
      private static final String TEST_SECRET_KEY =
              "dGVzdF9rZXlfMTIzNDU2Nzg5MGFiY2RlZmdoaWprbG0="; // test_key_1234567890abcdefghijklm (32

      // bytes in Base64)

      @BeforeEach
      void setUp() throws Exception {
          aesCryptoUtil = new AesCryptoUtil();
          setPrivateField(aesCryptoUtil, "secretKeyString", TEST_SECRET_KEY);
          aesCryptoUtil.init();
      }

      private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
          Field field = target.getClass().getDeclaredField(fieldName);
          field.setAccessible(true);
          field.set(target, value);
      }

      @Nested
      @DisplayName("초기화 테스트")
      class InitializationTest {

          @Test
          @DisplayName("올바른 키로 정상 초기화")
          void init_WithValidKey() throws Exception {
              // Given
              AesCryptoUtil util = new AesCryptoUtil();
              String validKey = "dGVzdF9rZXlfMTIzNDU2Nzg5MGFiY2RlZmdoaWprbG0=";
              setPrivateField(util, "secretKeyString", validKey);

              // When & Then
              assertDoesNotThrow(() -> util.init());
          }

          @Test
          @DisplayName("null 키로 초기화시 예외 발생")
          void init_WithNullKey() throws Exception {
              // Given
              AesCryptoUtil util = new AesCryptoUtil();
              setPrivateField(util, "secretKeyString", null);

              // When & Then
              IllegalStateException exception =
                      assertThrows(IllegalStateException.class, () -> util.init());
              assertTrue(exception.getMessage().contains("AES 암호화 키가 설정되지 않았습니다"));
          }

          @Test
          @DisplayName("빈 문자열 키로 초기화시 예외 발생")
          void init_WithEmptyKey() throws Exception {
              // Given
              AesCryptoUtil util = new AesCryptoUtil();
              setPrivateField(util, "secretKeyString", "");

              // When & Then
              IllegalStateException exception =
                      assertThrows(IllegalStateException.class, () -> util.init());
              assertTrue(exception.getMessage().contains("AES 암호화 키가 설정되지 않았습니다"));
          }

          @Test
          @DisplayName("잘못된 Base64 키로 초기화시 예외 발생")
          void init_WithInvalidBase64Key() throws Exception {
              // Given
              AesCryptoUtil util = new AesCryptoUtil();
              setPrivateField(util, "secretKeyString", "invalid-base64-key");

              // When & Then
              IllegalStateException exception =
                      assertThrows(IllegalStateException.class, () -> util.init());
              assertTrue(exception.getMessage().contains("잘못된 Base64 형식의 AES 키입니다"));
          }

          @Test
          @DisplayName("잘못된 길이의 키로 초기화시 예외 발생")
          void init_WithWrongKeyLength() throws Exception {
              // Given
              AesCryptoUtil util = new AesCryptoUtil();
              String shortKey = Base64.getEncoder().encodeToString("short".getBytes()); // 5 bytes
              setPrivateField(util, "secretKeyString", shortKey);

              // When & Then
              IllegalStateException exception =
                      assertThrows(IllegalStateException.class, () -> util.init());
              assertTrue(exception.getMessage().contains("AES 키는 256비트(32바이트)여야 합니다"));
          }
      }

      @Nested
      @DisplayName("encrypt 메서드 테스트")
      class EncryptTest {

          @Test
          @DisplayName("정상 문자열 암호화")
          void encrypt_ValidString() {
              // Given
              String plainText = "테스트 데이터입니다";

              // When
              String encrypted = aesCryptoUtil.encrypt(plainText);

              // Then
              assertNotNull(encrypted);
              assertNotEquals(plainText, encrypted);
              assertTrue(encrypted.length() > 0);

              // Base64로 디코딩 가능한지 확인
              assertDoesNotThrow(() -> Base64.getDecoder().decode(encrypted));
          }

          @Test
          @DisplayName("영문 문자열 암호화")
          void encrypt_EnglishString() {
              // Given
              String plainText = "Hello World! This is a test message.";

              // When
              String encrypted = aesCryptoUtil.encrypt(plainText);

              // Then
              assertNotNull(encrypted);
              assertNotEquals(plainText, encrypted);
          }

          @Test
          @DisplayName("특수문자 포함 문자열 암호화")
          void encrypt_SpecialCharacters() {
              // Given
              String plainText = "!@#$%^&*()_+-=[]{}|;':\",./<>?";

              // When
              String encrypted = aesCryptoUtil.encrypt(plainText);

              // Then
              assertNotNull(encrypted);
              assertNotEquals(plainText, encrypted);
          }

          @Test
          @DisplayName("긴 문자열 암호화")
          void encrypt_LongString() {
              // Given
              String plainText = "가".repeat(1000);

              // When
              String encrypted = aesCryptoUtil.encrypt(plainText);

              // Then
              assertNotNull(encrypted);
              assertNotEquals(plainText, encrypted);
          }

          @ParameterizedTest
          @NullAndEmptySource
          @DisplayName("null 또는 빈 문자열 암호화시 null 반환")
          void encrypt_NullOrEmpty(String plainText) {
              // When
              String encrypted = aesCryptoUtil.encrypt(plainText);

              // Then
              assertNull(encrypted);
          }

          @Test
          @DisplayName("동일한 평문을 여러번 암호화하면 다른 결과 생성")
          void encrypt_SameTextDifferentResults() {
              // Given
              String plainText = "동일한 텍스트";

              // When
              String encrypted1 = aesCryptoUtil.encrypt(plainText);
              String encrypted2 = aesCryptoUtil.encrypt(plainText);

              // Then
              assertNotNull(encrypted1);
              assertNotNull(encrypted2);
              assertNotEquals(encrypted1, encrypted2, "같은 평문이라도 IV가 다르므로 암호문이 달라야 함");
          }
      }

      @Nested
      @DisplayName("decrypt 메서드 테스트")
      class DecryptTest {

          @Test
          @DisplayName("정상 복호화")
          void decrypt_ValidEncryptedString() {
              // Given
              String originalText = "복호화 테스트 데이터";
              String encrypted = aesCryptoUtil.encrypt(originalText);

              // When
              String decrypted = aesCryptoUtil.decrypt(encrypted);

              // Then
              assertEquals(originalText, decrypted);
          }

          @Test
          @DisplayName("다양한 문자열 암호화/복호화")
          void decrypt_VariousStrings() {
              // Given
              String[] testStrings = {
                  "한글 테스트", "English Test", "123456789", "!@#$%^&*()", "Mixed한글English123!@#"
              };

              for (String original : testStrings) {
                  // When
                  String encrypted = aesCryptoUtil.encrypt(original);
                  String decrypted = aesCryptoUtil.decrypt(encrypted);

                  // Then
                  assertEquals(original, decrypted, "원본과 복호화된 문자열이 일치해야 함: " + original);
              }
          }

          @ParameterizedTest
          @NullAndEmptySource
          @DisplayName("null 또는 빈 문자열 복호화시 null 반환")
          void decrypt_NullOrEmpty(String encryptedText) {
              // When
              String decrypted = aesCryptoUtil.decrypt(encryptedText);

              // Then
              assertNull(decrypted);
          }

          @Test
          @DisplayName("잘못된 Base64 데이터 복호화시 예외 발생")
          void decrypt_InvalidBase64() {
              // Given
              String invalidBase64 = "invalid-base64-data";

              // When & Then
              RuntimeException exception =
                      assertThrows(
                              RuntimeException.class, () -> aesCryptoUtil.decrypt(invalidBase64));
              assertTrue(exception.getMessage().contains("잘못된 암호화 데이터 형식입니다"));
          }

          @Test
          @DisplayName("너무 짧은 암호화 데이터로 복호화시 예외 발생")
          void decrypt_TooShortData() {
              // Given
              String shortData = Base64.getEncoder().encodeToString("short".getBytes());

              // When & Then
              RuntimeException exception =
                      assertThrows(RuntimeException.class, () -> aesCryptoUtil.decrypt(shortData));
              assertTrue(exception.getMessage().contains("잘못된 암호화 데이터 형식입니다"));
          }

          @Test
          @DisplayName("변조된 암호화 데이터 복호화시 예외 발생")
          void decrypt_TamperedData() {
              // Given
              String originalText = "테스트 데이터";
              String encrypted = aesCryptoUtil.encrypt(originalText);

              // 암호화된 데이터의 마지막 문자를 변경하여 변조
              String tamperedData = encrypted.substring(0, encrypted.length() - 1) + "X";

              // When & Then
              RuntimeException exception =
                      assertThrows(RuntimeException.class, () -> aesCryptoUtil.decrypt(tamperedData));
              assertTrue(exception.getMessage().contains("데이터 복호화에 실패했습니다"));
          }
      }

      @Nested
      @DisplayName("isEncrypted 메서드 테스트")
      class IsEncryptedTest {

          @Test
          @DisplayName("암호화된 데이터 확인")
          void isEncrypted_ValidEncryptedData() {
              // Given
              String plainText = "테스트 데이터";
              String encrypted = aesCryptoUtil.encrypt(plainText);

              // When
              boolean result = aesCryptoUtil.isEncrypted(encrypted);

              // Then
              assertTrue(result);
          }

          @Test
          @DisplayName("평문 데이터 확인")
          void isEncrypted_PlainText() {
              // Given
              String plainText = "이것은 평문입니다";

              // When
              boolean result = aesCryptoUtil.isEncrypted(plainText);

              // Then
              assertFalse(result);
          }

          @ParameterizedTest
          @NullAndEmptySource
          @DisplayName("null 또는 빈 문자열은 암호화되지 않은 것으로 판단")
          void isEncrypted_NullOrEmpty(String text) {
              // When
              boolean result = aesCryptoUtil.isEncrypted(text);

              // Then
              assertFalse(result);
          }

          @Test
          @DisplayName("잘못된 Base64 형식은 암호화되지 않은 것으로 판단")
          void isEncrypted_InvalidBase64() {
              // Given
              String invalidBase64 = "invalid-base64-data@#$";

              // When
              boolean result = aesCryptoUtil.isEncrypted(invalidBase64);

              // Then
              assertFalse(result);
          }

          @Test
          @DisplayName("너무 짧은 Base64 데이터는 암호화되지 않은 것으로 판단")
          void isEncrypted_TooShortBase64() {
              // Given
              String shortBase64 = Base64.getEncoder().encodeToString("short".getBytes());

              // When
              boolean result = aesCryptoUtil.isEncrypted(shortBase64);

              // Then
              assertFalse(result);
          }

          @ParameterizedTest
          @ValueSource(
                  strings = {"일반 텍스트", "Regular text", "12345", "user@example.com", "010-1234-5678"})
          @DisplayName("다양한 평문들이 암호화되지 않은 것으로 판단")
          void isEncrypted_VariousPlainTexts(String plainText) {
              // When
              boolean result = aesCryptoUtil.isEncrypted(plainText);

              // Then
              assertFalse(result);
          }
      }

      @Nested
      @DisplayName("generateRandomKey 정적 메서드 테스트")
      class GenerateRandomKeyTest {

          @Test
          @DisplayName("랜덤 키 생성")
          void generateRandomKey_Success() {
              // When
              String key1 = AesCryptoUtil.generateRandomKey();
              String key2 = AesCryptoUtil.generateRandomKey();

              // Then
              assertNotNull(key1);
              assertNotNull(key2);
              assertNotEquals(key1, key2, "매번 다른 키가 생성되어야 함");

              // Base64로 디코딩 가능한지 확인
              assertDoesNotThrow(() -> Base64.getDecoder().decode(key1));
              assertDoesNotThrow(() -> Base64.getDecoder().decode(key2));

              // 32바이트(256비트) 키인지 확인
              byte[] decodedKey1 = Base64.getDecoder().decode(key1);
              byte[] decodedKey2 = Base64.getDecoder().decode(key2);
              assertEquals(32, decodedKey1.length);
              assertEquals(32, decodedKey2.length);
          }

          @Test
          @DisplayName("생성된 키로 AES 유틸리티 초기화 가능")
          void generateRandomKey_CanInitializeUtil() throws Exception {
              // Given
              String generatedKey = AesCryptoUtil.generateRandomKey();
              AesCryptoUtil util = new AesCryptoUtil();
              setPrivateField(util, "secretKeyString", generatedKey);

              // When & Then
              assertDoesNotThrow(() -> util.init());
          }

          @Test
          @DisplayName("생성된 키로 암호화/복호화 동작 확인")
          void generateRandomKey_CanEncryptDecrypt() throws Exception {
              // Given
              String generatedKey = AesCryptoUtil.generateRandomKey();
              AesCryptoUtil util = new AesCryptoUtil();
              setPrivateField(util, "secretKeyString", generatedKey);
              util.init();

              String testData = "생성된 키로 테스트";

              // When
              String encrypted = util.encrypt(testData);
              String decrypted = util.decrypt(encrypted);

              // Then
              assertEquals(testData, decrypted);
          }
      }

      @Nested
      @DisplayName("암호화/복호화 통합 테스트")
      class IntegrationTest {

          @Test
          @DisplayName("개인정보 데이터 암호화/복호화")
          void encryptDecrypt_PersonalData() {
              // Given
              String[] personalData = {
                  "홍길동", "010-1234-5678", "123456-1234567", "서울특별시 강남구 테헤란로 123", "hong@example.com"
              };

              for (String data : personalData) {
                  // When
                  String encrypted = aesCryptoUtil.encrypt(data);
                  String decrypted = aesCryptoUtil.decrypt(encrypted);

                  // Then
                  assertEquals(data, decrypted, "개인정보 데이터가 정확히 복원되어야 함: " + data);
                  assertTrue(aesCryptoUtil.isEncrypted(encrypted), "암호화된 데이터로 인식되어야 함");
                  assertFalse(aesCryptoUtil.isEncrypted(data), "원본 데이터는 평문으로 인식되어야 함");
              }
          }

          @Test
          @DisplayName("빈 문자열과 공백 처리")
          void encryptDecrypt_EmptyAndWhitespace() {
              // Given
              String[] testStrings = {
                  " ", // 공백 1개
                  "  ", // 공백 2개
                  "\t", // 탭
                  "\n", // 개행
                  "\r\n", // CRLF
                  "   test   " // 앞뒤 공백
              };

              for (String data : testStrings) {
                  // When
                  String encrypted = aesCryptoUtil.encrypt(data);
                  String decrypted = aesCryptoUtil.decrypt(encrypted);

                  // Then
                  assertEquals(data, decrypted, "공백 문자가 정확히 보존되어야 함");
              }
          }

          @Test
          @DisplayName("UTF-8 다국어 문자 처리")
          void encryptDecrypt_MultiLanguage() {
              // Given
              String[] multiLanguageData = {
                  "한글 테스트 データ",
                  "🔐 이모지 테스트 🚀",
                  "中文测试 العربية тест",
                  "日本語テスト हिन्दी परीक्षा",
                  "Ελληνικά ทดสอบภาษาไทย"
              };

              for (String data : multiLanguageData) {
                  // When
                  String encrypted = aesCryptoUtil.encrypt(data);
                  String decrypted = aesCryptoUtil.decrypt(encrypted);

                  // Then
                  assertEquals(data, decrypted, "다국어 문자가 정확히 복원되어야 함: " + data);
              }
          }

          @Test
          @DisplayName("대용량 데이터 암호화/복호화")
          void encryptDecrypt_LargeData() {
              // Given
              StringBuilder largeText = new StringBuilder();
              for (int i = 0; i < 10000; i++) {
                  largeText.append("대용량 테스트 데이터 ").append(i).append("\n");
              }
              String originalText = largeText.toString();

              // When
              String encrypted = aesCryptoUtil.encrypt(originalText);
              String decrypted = aesCryptoUtil.decrypt(encrypted);

              // Then
              assertEquals(originalText, decrypted);
              assertTrue(aesCryptoUtil.isEncrypted(encrypted));
          }

          @Test
          @DisplayName("연속 암호화/복호화 작업")
          void encryptDecrypt_MultipleOperations() {
              // Given
              String originalData = "연속 작업 테스트";

              // When
              for (int i = 0; i < 100; i++) {
                  String encrypted = aesCryptoUtil.encrypt(originalData);
                  String decrypted = aesCryptoUtil.decrypt(encrypted);

                  // Then
                  assertEquals(originalData, decrypted, "연속 작업에서도 정확한 결과를 보장해야 함");
              }
          }
      }

      @Nested
      @DisplayName("보안 검증 테스트")
      class SecurityVerificationTest {

          @Test
          @DisplayName("암호문에 평문이 포함되지 않음 확인")
          void encrypt_NoPlainTextInCiphertext() {
              // Given
              String[] sensitiveData = {"비밀번호123", "신용카드번호", "주민등록번호", "계좌번호"};

              for (String data : sensitiveData) {
                  // When
                  String encrypted = aesCryptoUtil.encrypt(data);

                  // Then
                  assertFalse(encrypted.contains(data), "암호문에 평문이 포함되면 안됨: " + data);
              }
          }

          @Test
          @DisplayName("동일한 데이터의 반복 암호화시 IV 랜덤성 확인")
          void encrypt_IVRandomness() {
              // Given
              String testData = "IV 랜덤성 테스트";

              // When
              String[] encryptedResults = new String[10];
              for (int i = 0; i < 10; i++) {
                  encryptedResults[i] = aesCryptoUtil.encrypt(testData);
              }

              // Then
              // 모든 암호문이 서로 달라야 함 (IV가 랜덤하게 생성되므로)
              for (int i = 0; i < encryptedResults.length; i++) {
                  for (int j = i + 1; j < encryptedResults.length; j++) {
                      assertNotEquals(
                              encryptedResults[i],
                              encryptedResults[j],
                              "같은 평문이라도 IV가 다르므로 암호문이 달라야 함");
                  }
              }

              // 모든 암호문이 복호화되면 같은 평문이 나와야 함
              for (String encrypted : encryptedResults) {
                  String decrypted = aesCryptoUtil.decrypt(encrypted);
                  assertEquals(testData, decrypted);
              }
          }
      }
}
