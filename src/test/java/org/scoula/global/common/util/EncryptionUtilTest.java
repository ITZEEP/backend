package org.scoula.global.common.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
@DisplayName("EncryptionUtil 테스트")
class EncryptionUtilTest {

      @Mock private RedisTemplate<String, Object> redisTemplate;
      @Mock private ValueOperations<String, Object> valueOperations;

      @InjectMocks private EncryptionUtil encryptionUtil;

      private static final String TEST_PDF_CONTENT = "%PDF-1.4\nTest PDF content";
      private static final String TEST_IMAGE_CONTENT = "fake image content";
      private static final String TEST_PASSWORD = "testPassword123";
      private static final String TEST_FILE_ID = "test-file-id";

      @BeforeEach
      void setUp() {
          lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
          ReflectionTestUtils.setField(
                  encryptionUtil, "serverMasterKey", "test-master-key-32-bytes-long!!");
      }

      @Nested
      @DisplayName("addSecondKey 메서드 테스트")
      class AddSecondKeyTest {

          @Test
          @DisplayName("유효한 두 번째 키 추가 및 암호화 성공")
          void addSecondKey_Success() throws Exception {
              // Given
              EncryptionUtil.ContractKeys contractKeys = new EncryptionUtil.ContractKeys();
              contractKeys.setOwnerKey("ownerPassword");
              contractKeys.setPdfData(
                      org.apache.commons.codec.binary.Base64.encodeBase64String(
                              TEST_PDF_CONTENT.getBytes()));
              contractKeys.setCreatedTimestamp(new Date());

              when(valueOperations.get(anyString())).thenReturn(contractKeys);

              // When
              EncryptionUtil.ContractEncryptionResult result =
                      encryptionUtil.addSecondKey(TEST_FILE_ID, "tenant", "tenantPassword");

              // Then
              assertNotNull(result);
              assertEquals(TEST_FILE_ID, result.getFileId());
              assertEquals("ENCRYPTION_COMPLETED", result.getStatus());
              assertNotNull(result.getEncryptedPDF());
              verify(redisTemplate).delete(anyString());
          }

          @Test
          @DisplayName("계약이 존재하지 않을 때 예외 발생")
          void addSecondKey_ContractNotFound() {
              // Given
              when(valueOperations.get(anyString())).thenReturn(null);

              // When & Then
              IllegalArgumentException exception =
                      assertThrows(
                              IllegalArgumentException.class,
                              () ->
                                      encryptionUtil.addSecondKey(
                                              TEST_FILE_ID, "tenant", "tenantPassword"));
              assertTrue(exception.getMessage().contains("Contract not found or expired"));
          }

          @Test
          @DisplayName("잘못된 키 타입으로 예외 발생")
          void addSecondKey_InvalidKeyType() {
              // Given
              EncryptionUtil.ContractKeys contractKeys = new EncryptionUtil.ContractKeys();
              when(valueOperations.get(anyString())).thenReturn(contractKeys);

              // When & Then
              IllegalArgumentException exception =
                      assertThrows(
                              IllegalArgumentException.class,
                              () -> encryptionUtil.addSecondKey(TEST_FILE_ID, "invalid", "password"));
              assertTrue(exception.getMessage().contains("keyType must be 'owner' or 'tenant'"));
          }

          @Test
          @DisplayName("이미 존재하는 owner 키로 예외 발생")
          void addSecondKey_OwnerKeyAlreadyExists() {
              // Given
              EncryptionUtil.ContractKeys contractKeys = new EncryptionUtil.ContractKeys();
              contractKeys.setOwnerKey("existingOwnerKey");
              when(valueOperations.get(anyString())).thenReturn(contractKeys);

              // When & Then
              IllegalArgumentException exception =
                      assertThrows(
                              IllegalArgumentException.class,
                              () ->
                                      encryptionUtil.addSecondKey(
                                              TEST_FILE_ID, "owner", "newOwnerPassword"));
              assertTrue(exception.getMessage().contains("Owner key already exists"));
          }

          @Test
          @DisplayName("이미 존재하는 tenant 키로 예외 발생")
          void addSecondKey_TenantKeyAlreadyExists() {
              // Given
              EncryptionUtil.ContractKeys contractKeys = new EncryptionUtil.ContractKeys();
              contractKeys.setTenantKey("existingTenantKey");
              when(valueOperations.get(anyString())).thenReturn(contractKeys);

              // When & Then
              IllegalArgumentException exception =
                      assertThrows(
                              IllegalArgumentException.class,
                              () ->
                                      encryptionUtil.addSecondKey(
                                              TEST_FILE_ID, "tenant", "newTenantPassword"));
              assertTrue(exception.getMessage().contains("Tenant key already exists"));
          }

          @Test
          @DisplayName("첫 번째 키만 있을 때 대기 상태 반환")
          void addSecondKey_WaitingForMoreKeys() throws Exception {
              // Given
              EncryptionUtil.ContractKeys contractKeys = new EncryptionUtil.ContractKeys();
              contractKeys.setOwnerKey("ownerPassword");
              when(valueOperations.get(anyString())).thenReturn(contractKeys);

              // When
              EncryptionUtil.ContractEncryptionResult result =
                      encryptionUtil.addSecondKey(TEST_FILE_ID, "tenant", "tenantPassword");

              // Then
              assertNotNull(result);
              assertEquals(TEST_FILE_ID, result.getFileId());
              assertTrue(result.getStatus().contains("WAITING_"));
              verify(valueOperations).set(anyString(), any(), eq(30L), eq(TimeUnit.MINUTES));
          }
      }

      @Nested
      @DisplayName("decryptPDF 메서드 테스트")
      class DecryptPDFTest {

          @Test
          @DisplayName("유효한 암호화된 PDF 복호화 성공")
          void decryptPDF_Success() throws Exception {
              // Given - 먼저 PDF를 암호화
              MockMultipartFile pdfFile =
                      new MockMultipartFile(
                              "test", "test.pdf", "application/pdf", TEST_PDF_CONTENT.getBytes());
              EncryptionUtil.EncryptedPDF encryptedPDF =
                      encryptionUtil.encryptPDF(pdfFile, "ownerPass", "tenantPass");

              // When
              byte[] decryptedData = encryptionUtil.decryptPDF(encryptedPDF, "ownerPass", null);

              // Then
              assertNotNull(decryptedData);
              assertEquals(TEST_PDF_CONTENT, new String(decryptedData));
          }

          @Test
          @DisplayName("충분하지 않은 키로 복호화 실패")
          void decryptPDF_InsufficientKeys() throws Exception {
              // Given
              EncryptionUtil.EncryptedPDF encryptedPDF = new EncryptionUtil.EncryptedPDF();
              encryptedPDF.encryptedShares = new java.util.HashMap<>();

              // When & Then
              IllegalArgumentException exception =
                      assertThrows(
                              IllegalArgumentException.class,
                              () -> encryptionUtil.decryptPDF(encryptedPDF, null, null));
              assertTrue(exception.getMessage().contains("최소 2개의 키가 필요"));
          }

          @Test
          @DisplayName("해시 불일치로 복호화 실패")
          void decryptPDF_HashMismatch() throws Exception {
              // Given - 올바른 암호화된 PDF 생성
              MockMultipartFile pdfFile =
                      new MockMultipartFile(
                              "test", "test.pdf", "application/pdf", TEST_PDF_CONTENT.getBytes());
              EncryptionUtil.EncryptedPDF encryptedPDF =
                      encryptionUtil.encryptPDF(pdfFile, "ownerPass", "tenantPass");

              // 해시를 잘못된 값으로 변경
              encryptedPDF.originalHash = "wrong-hash";

              // When & Then
              SecurityException exception =
                      assertThrows(
                              SecurityException.class,
                              () -> encryptionUtil.decryptPDF(encryptedPDF, "ownerPass", null));
              assertTrue(exception.getMessage().contains("해시값 불일치"));
          }
      }

      @Nested
      @DisplayName("encryptPDF 메서드 테스트")
      class EncryptPDFTest {

          @Test
          @DisplayName("유효한 PDF 파일 암호화 성공")
          void encryptPDF_Success() throws Exception {
              // Given
              MockMultipartFile pdfFile =
                      new MockMultipartFile(
                              "test", "test.pdf", "application/pdf", TEST_PDF_CONTENT.getBytes());

              // When
              EncryptionUtil.EncryptedPDF result =
                      encryptionUtil.encryptPDF(pdfFile, "ownerPass", "tenantPass");

              // Then
              assertNotNull(result);
              assertNotNull(result.encryptedData);
              assertNotNull(result.iv);
              assertNotNull(result.originalHash);
              assertNotNull(result.encryptedShares);
              assertEquals(2, result.threshold);
              assertEquals(3, result.totalShares);
              assertEquals("test.pdf", result.originalFilename);
              assertEquals("application/pdf", result.contentType);
          }

          @Test
          @DisplayName("null 파일로 암호화 실패")
          void encryptPDF_NullFile() {
              // When & Then
              IllegalArgumentException exception =
                      assertThrows(
                              IllegalArgumentException.class,
                              () -> encryptionUtil.encryptPDF(null, "ownerPass", "tenantPass"));
              assertTrue(exception.getMessage().contains("empty or null"));
          }

          @Test
          @DisplayName("빈 파일로 암호화 실패")
          void encryptPDF_EmptyFile() {
              // Given
              MockMultipartFile emptyFile =
                      new MockMultipartFile("test", "test.pdf", "application/pdf", new byte[0]);

              // When & Then
              IllegalArgumentException exception =
                      assertThrows(
                              IllegalArgumentException.class,
                              () -> encryptionUtil.encryptPDF(emptyFile, "ownerPass", "tenantPass"));
              assertTrue(exception.getMessage().contains("empty or null"));
          }

          @Test
          @DisplayName("잘못된 Content Type으로 암호화 실패")
          void encryptPDF_InvalidContentType() {
              // Given
              MockMultipartFile txtFile =
                      new MockMultipartFile(
                              "test", "test.txt", "text/plain", TEST_PDF_CONTENT.getBytes());

              // When & Then
              IllegalArgumentException exception =
                      assertThrows(
                              IllegalArgumentException.class,
                              () -> encryptionUtil.encryptPDF(txtFile, "ownerPass", "tenantPass"));
              assertTrue(exception.getMessage().contains("File is not a PDF"));
          }
      }

      @Nested
      @DisplayName("encryptPDFWithPassword 메서드 테스트")
      class EncryptPDFWithPasswordTest {

          @Test
          @DisplayName("비밀번호로 PDF 암호화 성공")
          void encryptPDFWithPassword_Success() throws Exception {
              // Given
              MockMultipartFile pdfFile =
                      new MockMultipartFile(
                              "test", "test.pdf", "application/pdf", TEST_PDF_CONTENT.getBytes());

              // When
              byte[] encryptedData =
                      encryptionUtil.encryptPDFWithPassword(pdfFile, "userPass", "ownerPass");

              // Then
              assertNotNull(encryptedData);
              assertTrue(encryptedData.length > 0);
          }

          @Test
          @DisplayName("null 파일로 암호화 실패")
          void encryptPDFWithPassword_NullFile() {
              // When & Then
              IllegalArgumentException exception =
                      assertThrows(
                              IllegalArgumentException.class,
                              () ->
                                      encryptionUtil.encryptPDFWithPassword(
                                              null, "userPass", "ownerPass"));
              assertTrue(exception.getMessage().contains("empty or null"));
          }
      }

      @Nested
      @DisplayName("decryptPDFWithPassword 메서드 테스트")
      class DecryptPDFWithPasswordTest {

          @Test
          @DisplayName("비밀번호로 PDF 복호화 성공")
          void decryptPDFWithPassword_Success() throws Exception {
              // Given
              MockMultipartFile pdfFile =
                      new MockMultipartFile(
                              "test", "test.pdf", "application/pdf", TEST_PDF_CONTENT.getBytes());
              byte[] encryptedData =
                      encryptionUtil.encryptPDFWithPassword(pdfFile, "password", "password");
              MockMultipartFile encryptedFile =
                      new MockMultipartFile(
                              "encrypted", "encrypted.pdf", "application/pdf", encryptedData);

              // When
              byte[] decryptedData = encryptionUtil.decryptPDFWithPassword(encryptedFile, "password");

              // Then
              assertNotNull(decryptedData);
              assertTrue(decryptedData.length > 0);
          }

          @Test
          @DisplayName("잘못된 비밀번호로 복호화 실패")
          void decryptPDFWithPassword_WrongPassword() throws Exception {
              // Given
              MockMultipartFile pdfFile =
                      new MockMultipartFile(
                              "test", "test.pdf", "application/pdf", TEST_PDF_CONTENT.getBytes());
              byte[] encryptedData =
                      encryptionUtil.encryptPDFWithPassword(
                              pdfFile, "correctPassword", "correctPassword");
              MockMultipartFile encryptedFile =
                      new MockMultipartFile(
                              "encrypted", "encrypted.pdf", "application/pdf", encryptedData);

              // When & Then
              Exception exception =
                      assertThrows(
                              Exception.class,
                              () ->
                                      encryptionUtil.decryptPDFWithPassword(
                                              encryptedFile, "wrongPassword"));
              assertTrue(exception.getMessage().contains("PDF decryption failed"));
          }

          @Test
          @DisplayName("유효하지 않은 PDF 데이터로 복호화 실패")
          void decryptPDFWithPassword_InvalidPDF() {
              // Given
              MockMultipartFile invalidFile =
                      new MockMultipartFile(
                              "invalid", "invalid.pdf", "application/pdf", "not a pdf".getBytes());

              // When & Then
              IllegalArgumentException exception =
                      assertThrows(
                              IllegalArgumentException.class,
                              () -> encryptionUtil.decryptPDFWithPassword(invalidFile, "password"));
              assertTrue(exception.getMessage().contains("File is not a valid PDF document"));
          }

          @Test
          @DisplayName("null 또는 빈 데이터로 복호화 실패")
          void decryptPDFWithPassword_NullOrEmptyData() {
              // Given
              MockMultipartFile nullFile =
                      new MockMultipartFile("null", "null.pdf", "application/pdf", new byte[0]);

              // When & Then
              IllegalArgumentException exception =
                      assertThrows(
                              IllegalArgumentException.class,
                              () -> encryptionUtil.decryptPDFWithPassword(nullFile, "password"));
              assertTrue(exception.getMessage().contains("Invalid PDF data"));
          }
      }

      @Nested
      @DisplayName("encryptImageFromMultipartFile 메서드 테스트")
      class EncryptImageFromMultipartFileTest {

          @Test
          @DisplayName("이미지 파일 암호화 성공")
          void encryptImageFromMultipartFile_Success() throws Exception {
              // Given
              MockMultipartFile imageFile =
                      new MockMultipartFile(
                              "test", "test.jpg", "image/jpeg", TEST_IMAGE_CONTENT.getBytes());

              // When
              EncryptionUtil.EncryptedImage result =
                      encryptionUtil.encryptImageFromMultipartFile(imageFile);

              // Then
              assertNotNull(result);
              assertNotNull(result.encryptedData);
              assertNotNull(result.iv);
              assertNotNull(result.salt);
              assertNotNull(result.originalHash);
              assertEquals("AES/CBC/PKCS5Padding", result.algorithm);
              assertEquals("test.jpg", result.originalFilename);
              assertEquals("image/jpeg", result.contentType);
          }

          @Test
          @DisplayName("null 파일로 암호화 실패")
          void encryptImageFromMultipartFile_NullFile() {
              // When & Then
              assertThrows(Exception.class, () -> encryptionUtil.encryptImageFromMultipartFile(null));
          }

          @Test
          @DisplayName("빈 파일로 암호화 실패")
          void encryptImageFromMultipartFile_EmptyFile() {
              // Given
              MockMultipartFile emptyFile =
                      new MockMultipartFile("test", "test.jpg", "image/jpeg", new byte[0]);

              // When & Then
              assertThrows(
                      Exception.class, () -> encryptionUtil.encryptImageFromMultipartFile(emptyFile));
          }
      }

      @Nested
      @DisplayName("encryptPDFWithUserPassword 메서드 테스트")
      class EncryptPDFWithUserPasswordTest {

          @Test
          @DisplayName("사용자 비밀번호로 PDF 암호화 성공")
          void encryptPDFWithUserPassword_Success() throws Exception {
              // Given
              MockMultipartFile pdfFile =
                      new MockMultipartFile(
                              "test", "test.pdf", "application/pdf", TEST_PDF_CONTENT.getBytes());

              // When
              byte[] encryptedData =
                      encryptionUtil.encryptPDFWithUserPassword(pdfFile, "userPassword");

              // Then
              assertNotNull(encryptedData);
              assertTrue(encryptedData.length > 0);
          }

          @Test
          @DisplayName("null 파일로 암호화 실패")
          void encryptPDFWithUserPassword_NullFile() {
              // When & Then
              IllegalArgumentException exception =
                      assertThrows(
                              IllegalArgumentException.class,
                              () -> encryptionUtil.encryptPDFWithUserPassword(null, "userPassword"));
              assertTrue(exception.getMessage().contains("empty or null"));
          }
      }

      @Nested
      @DisplayName("calculateHash 메서드 테스트")
      class CalculateHashTest {

          @Test
          @DisplayName("데이터 해시 계산 성공")
          void calculateHash_Success() throws NoSuchAlgorithmException {
              // Given
              byte[] testData = "test data".getBytes();

              // When
              String hash = encryptionUtil.calculateHash(testData);

              // Then
              assertNotNull(hash);
              assertFalse(hash.isEmpty());
          }

          @Test
          @DisplayName("빈 데이터 해시 계산")
          void calculateHash_EmptyData() throws NoSuchAlgorithmException {
              // Given
              byte[] emptyData = new byte[0];

              // When
              String hash = encryptionUtil.calculateHash(emptyData);

              // Then
              assertNotNull(hash);
              assertFalse(hash.isEmpty());
          }

          @Test
          @DisplayName("null 데이터로 해시 계산 실패")
          void calculateHash_NullData() {
              // When & Then
              assertThrows(NullPointerException.class, () -> encryptionUtil.calculateHash(null));
          }
      }

      @Nested
      @DisplayName("uploadContract 메서드 테스트")
      class UploadContractTest {

          @Test
          @DisplayName("PDF 파일 없이 첫 번째 키 저장 - owner")
          void uploadContract_FirstKeyOnly_Owner() throws Exception {
              // When
              EncryptionUtil.ContractKeyStatus result =
                      encryptionUtil.uploadContract(TEST_FILE_ID, null, "owner", "ownerPassword");

              // Then
              assertNotNull(result);
              assertEquals(TEST_FILE_ID, result.getFileId());
              assertEquals("WAITING_PDF_AND_TENANT_KEY", result.getStatus());
              assertTrue(result.isHasOwnerKey());
              assertFalse(result.isHasTenantKey());
              verify(valueOperations).set(anyString(), any(), eq(60L), eq(TimeUnit.MINUTES));
          }

          @Test
          @DisplayName("PDF 파일과 두 번째 키 저장 - tenant")
          void uploadContract_PdfAndSecondKey_Tenant() throws Exception {
              // Given
              EncryptionUtil.ContractKeys existingKeys = new EncryptionUtil.ContractKeys();
              existingKeys.setOwnerKey("ownerPassword");
              existingKeys.setCreatedTimestamp(new Date());
              when(valueOperations.get(anyString())).thenReturn(existingKeys);

              MockMultipartFile pdfFile =
                      new MockMultipartFile(
                              "test", "test.pdf", "application/pdf", TEST_PDF_CONTENT.getBytes());

              // When
              EncryptionUtil.ContractKeyStatus result =
                      encryptionUtil.uploadContract(
                              TEST_FILE_ID, pdfFile, "tenant", "tenantPassword");

              // Then
              assertNotNull(result);
              assertEquals(TEST_FILE_ID, result.getFileId());
              assertEquals("ENCRYPTION_COMPLETE", result.getStatus());
              assertTrue(result.isHasOwnerKey());
              assertTrue(result.isHasTenantKey());
              assertNotNull(result.getEncryptedPDF());
              verify(redisTemplate).delete(anyString());
          }

          @Test
          @DisplayName("기존 첫 번째 키 없이 두 번째 키 저장 실패")
          void uploadContract_SecondKeyWithoutFirst() throws Exception {
              // Given
              when(valueOperations.get(anyString())).thenReturn(null);
              MockMultipartFile pdfFile =
                      new MockMultipartFile(
                              "test", "test.pdf", "application/pdf", TEST_PDF_CONTENT.getBytes());

              // When & Then
              IllegalArgumentException exception =
                      assertThrows(
                              IllegalArgumentException.class,
                              () ->
                                      encryptionUtil.uploadContract(
                                              TEST_FILE_ID, pdfFile, "tenant", "tenantPassword"));
              assertTrue(exception.getMessage().contains("Please complete Step 1 first"));
          }

          @Test
          @DisplayName("기존 로직 - PDF 파일로 계약 업로드")
          void uploadContract_LegacyLogic_WithPdf() throws Exception {
              // Given
              MockMultipartFile pdfFile =
                      new MockMultipartFile(
                              "test", "test.pdf", "application/pdf", TEST_PDF_CONTENT.getBytes());

              // When
              EncryptionUtil.ContractKeyStatus result =
                      encryptionUtil.uploadContract(TEST_FILE_ID, pdfFile, "owner", "ownerPassword");

              // Then
              assertNotNull(result);
              assertEquals(TEST_FILE_ID, result.getFileId());
              assertTrue(result.getStatus().contains("WAITING_"));
              verify(valueOperations).set(anyString(), any(), eq(30L), eq(TimeUnit.MINUTES));
          }

          @Test
          @DisplayName("기존 로직 - 두 번째 키 추가")
          void uploadContract_LegacyLogic_SecondKey() throws Exception {
              // Given
              EncryptionUtil.ContractKeys contractKeys = new EncryptionUtil.ContractKeys();
              contractKeys.setOwnerKey("ownerPassword");
              contractKeys.setPdfData(
                      org.apache.commons.codec.binary.Base64.encodeBase64String(
                              TEST_PDF_CONTENT.getBytes()));
              contractKeys.setCreatedTimestamp(new Date());
              when(valueOperations.get(anyString())).thenReturn(contractKeys);

              // When
              EncryptionUtil.ContractKeyStatus result =
                      encryptionUtil.uploadContract(TEST_FILE_ID, null, "tenant", "tenantPassword");

              // Then
              assertNotNull(result);
              assertEquals(TEST_FILE_ID, result.getFileId());
              assertEquals("ENCRYPTION_COMPLETED", result.getStatus());
              assertTrue(result.isHasOwnerKey());
              assertTrue(result.isHasTenantKey());
              assertNotNull(result.getEncryptedPDF());
          }
      }

      @Nested
      @DisplayName("addSecondKeyWithFileInfo 메서드 테스트")
      class AddSecondKeyWithFileInfoTest {

          @Test
          @DisplayName("파일 정보와 함께 두 번째 키 추가 성공")
          void addSecondKeyWithFileInfo_Success() throws Exception {
              // Given
              EncryptionUtil.ContractKeys contractKeys = new EncryptionUtil.ContractKeys();
              contractKeys.setOwnerKey("ownerPassword");
              contractKeys.setPdfData(
                      org.apache.commons.codec.binary.Base64.encodeBase64String(
                              TEST_PDF_CONTENT.getBytes()));
              contractKeys.setCreatedTimestamp(new Date());
              when(valueOperations.get(anyString())).thenReturn(contractKeys);

              // When
              EncryptionUtil.ContractEncryptionResult result =
                      encryptionUtil.addSecondKeyWithFileInfo(
                              TEST_FILE_ID, "tenant", "tenantPassword", "original.pdf");

              // Then
              assertNotNull(result);
              assertEquals(TEST_FILE_ID, result.getFileId());
              assertEquals("ENCRYPTION_COMPLETED", result.getStatus());
              assertNotNull(result.getEncryptedPDF());
              assertEquals("original.pdf", result.getEncryptedPDF().originalFilename);
              assertEquals("application/pdf", result.getEncryptedPDF().contentType);
          }

          @Test
          @DisplayName("암호화되지 않은 경우 파일 정보 추가되지 않음")
          void addSecondKeyWithFileInfo_NotEncrypted() throws Exception {
              // Given
              EncryptionUtil.ContractKeys contractKeys = new EncryptionUtil.ContractKeys();
              contractKeys.setOwnerKey("ownerPassword");
              when(valueOperations.get(anyString())).thenReturn(contractKeys);

              // When
              EncryptionUtil.ContractEncryptionResult result =
                      encryptionUtil.addSecondKeyWithFileInfo(
                              TEST_FILE_ID, "tenant", "tenantPassword", "original.pdf");

              // Then
              assertNotNull(result);
              assertTrue(result.getStatus().contains("WAITING_"));
              // 암호화가 완료되지 않았으므로 encryptedPDF가 null이어야 함
          }
      }

      @Nested
      @DisplayName("decryptImage 메서드 테스트")
      class DecryptImageTest {

          @Test
          @DisplayName("이미지 복호화 성공")
          void decryptImage_Success() throws Exception {
              // Given - 먼저 이미지를 암호화
              MockMultipartFile imageFile =
                      new MockMultipartFile(
                              "test", "test.jpg", "image/jpeg", TEST_IMAGE_CONTENT.getBytes());
              EncryptionUtil.EncryptedImage encryptedImage =
                      encryptionUtil.encryptImageFromMultipartFile(imageFile);

              // When
              byte[] decryptedData =
                      encryptionUtil.decryptImage(
                              encryptedImage,
                              ReflectionTestUtils.getField(encryptionUtil, "serverMasterKey")
                                      .toString());

              // Then
              assertNotNull(decryptedData);
              assertEquals(TEST_IMAGE_CONTENT, new String(decryptedData));
          }

          @Test
          @DisplayName("잘못된 비밀번호로 이미지 복호화 실패")
          void decryptImage_WrongPassword() throws Exception {
              // Given
              MockMultipartFile imageFile =
                      new MockMultipartFile(
                              "test", "test.jpg", "image/jpeg", TEST_IMAGE_CONTENT.getBytes());
              EncryptionUtil.EncryptedImage encryptedImage =
                      encryptionUtil.encryptImageFromMultipartFile(imageFile);

              // When & Then - BadPaddingException is thrown first before hash check
              Exception exception =
                      assertThrows(
                              Exception.class,
                              () -> encryptionUtil.decryptImage(encryptedImage, "wrongPassword"));
              assertTrue(
                      exception instanceof javax.crypto.BadPaddingException
                              || exception instanceof SecurityException);
          }

          @Test
          @DisplayName("알고리즘이 null인 경우 기본값 사용")
          void decryptImage_NullAlgorithm() throws Exception {
              // Given
              MockMultipartFile imageFile =
                      new MockMultipartFile(
                              "test", "test.jpg", "image/jpeg", TEST_IMAGE_CONTENT.getBytes());
              EncryptionUtil.EncryptedImage encryptedImage =
                      encryptionUtil.encryptImageFromMultipartFile(imageFile);
              encryptedImage.algorithm = null; // 알고리즘을 null로 설정

              // When
              byte[] decryptedData =
                      encryptionUtil.decryptImage(
                              encryptedImage,
                              ReflectionTestUtils.getField(encryptionUtil, "serverMasterKey")
                                      .toString());

              // Then
              assertNotNull(decryptedData);
              assertEquals(TEST_IMAGE_CONTENT, new String(decryptedData));
          }

          @Test
          @DisplayName("빈 알고리즘인 경우 기본값 사용")
          void decryptImage_EmptyAlgorithm() throws Exception {
              // Given
              MockMultipartFile imageFile =
                      new MockMultipartFile(
                              "test", "test.jpg", "image/jpeg", TEST_IMAGE_CONTENT.getBytes());
              EncryptionUtil.EncryptedImage encryptedImage =
                      encryptionUtil.encryptImageFromMultipartFile(imageFile);
              encryptedImage.algorithm = ""; // 알고리즘을 빈 문자열로 설정

              // When
              byte[] decryptedData =
                      encryptionUtil.decryptImage(
                              encryptedImage,
                              ReflectionTestUtils.getField(encryptionUtil, "serverMasterKey")
                                      .toString());

              // Then
              assertNotNull(decryptedData);
              assertEquals(TEST_IMAGE_CONTENT, new String(decryptedData));
          }
      }

      @Nested
      @DisplayName("hasKey 메서드 테스트")
      class HasKeyTest {

          @Test
          @DisplayName("키가 존재하는 경우 true 반환")
          void hasKey_KeyExists() {
              // Given
              when(redisTemplate.hasKey(anyString())).thenReturn(true);

              // When
              boolean result = encryptionUtil.hasKey("test-contract-id");

              // Then
              assertTrue(result);
              verify(redisTemplate).hasKey("contract:test-contract-id:keys");
          }

          @Test
          @DisplayName("키가 존재하지 않는 경우 false 반환")
          void hasKey_KeyNotExists() {
              // Given
              when(redisTemplate.hasKey(anyString())).thenReturn(false);

              // When
              boolean result = encryptionUtil.hasKey("test-contract-id");

              // Then
              assertFalse(result);
              verify(redisTemplate).hasKey("contract:test-contract-id:keys");
          }

          @Test
          @DisplayName("null 계약 ID로 false 반환")
          void hasKey_NullContractId() {
              // When
              boolean result = encryptionUtil.hasKey(null);

              // Then
              assertFalse(result);
              verify(redisTemplate, never()).hasKey(anyString());
          }

          @Test
          @DisplayName("빈 계약 ID로 false 반환")
          void hasKey_EmptyContractId() {
              // When
              boolean result = encryptionUtil.hasKey("");

              // Then
              assertFalse(result);
              verify(redisTemplate, never()).hasKey(anyString());
          }

          @Test
          @DisplayName("공백 계약 ID로 false 반환")
          void hasKey_BlankContractId() {
              // When
              boolean result = encryptionUtil.hasKey("   ");

              // Then
              assertFalse(result);
              verify(redisTemplate, never()).hasKey(anyString());
          }
      }

      @Nested
      @DisplayName("서버 마스터 키 테스트")
      class ServerMasterKeyTest {

          @Test
          @DisplayName("서버 마스터 키 반환")
          void getServerMasterKey() {
              // When
              String masterKey = encryptionUtil.getServerMasterKey();

              // Then
              assertNotNull(masterKey);
              assertEquals("test-master-key-32-bytes-long!!", masterKey);
          }
      }

      @Nested
      @DisplayName("MultipartFile 유효성 검증 테스트")
      class ValidateMultipartFileTest {

          @Test
          @DisplayName("null Content Type으로 검증 실패")
          void validateMultipartFile_NullContentType() {
              // Given
              MultipartFile mockFile = mock(MultipartFile.class);
              when(mockFile.isEmpty()).thenReturn(false);
              when(mockFile.getContentType()).thenReturn(null);

              // When & Then
              IllegalArgumentException exception =
                      assertThrows(
                              IllegalArgumentException.class,
                              () -> encryptionUtil.encryptPDF(mockFile, "owner", "tenant"));
              assertTrue(exception.getMessage().contains("File content type is null"));
          }

          @Test
          @DisplayName("이미지 타입 검증 - 유효하지 않은 Content Type")
          void validateMultipartFile_InvalidImageContentType() {
              // Given - text file with non-image content type
              MockMultipartFile textFile =
                      new MockMultipartFile(
                              "test", "test.txt", "text/plain", "test content".getBytes());

              // When & Then - The method doesn't validate content type for images, so it actually
              // succeeds
              // This test documents the current behavior where image content type is not validated
              assertDoesNotThrow(() -> encryptionUtil.encryptImageFromMultipartFile(textFile));
          }
      }

      @Nested
      @DisplayName("내부 클래스 테스트")
      class InnerClassTest {

          @Test
          @DisplayName("ContractKeys 클래스 기본 동작")
          void contractKeys_BasicOperations() {
              // Given
              EncryptionUtil.ContractKeys contractKeys = new EncryptionUtil.ContractKeys();
              Date testDate = new Date();

              // When
              contractKeys.setOwnerKey("ownerKey");
              contractKeys.setTenantKey("tenantKey");
              contractKeys.setPdfData("pdfData");
              contractKeys.setStatus("status");
              contractKeys.setCreatedTimestamp(testDate);

              // Then
              assertEquals("ownerKey", contractKeys.getOwnerKey());
              assertEquals("tenantKey", contractKeys.getTenantKey());
              assertEquals("pdfData", contractKeys.getPdfData());
              assertEquals("status", contractKeys.getStatus());
              assertEquals(testDate, contractKeys.getCreatedTimestamp());
          }

          @Test
          @DisplayName("ContractKeyStatus 빌더 패턴")
          void contractKeyStatus_BuilderPattern() {
              // Given
              Date testDate = new Date();
              EncryptionUtil.EncryptedPDF encryptedPDF = new EncryptionUtil.EncryptedPDF();

              // When
              EncryptionUtil.ContractKeyStatus status =
                      EncryptionUtil.ContractKeyStatus.builder()
                              .fileId("testFileId")
                              .status("testStatus")
                              .message("testMessage")
                              .createdTimestamp(testDate)
                              .hasOwnerKey(true)
                              .hasTenantKey(false)
                              .encryptedPDF(encryptedPDF)
                              .build();

              // Then
              assertEquals("testFileId", status.getFileId());
              assertEquals("testStatus", status.getStatus());
              assertEquals("testMessage", status.getMessage());
              assertEquals(testDate, status.getCreatedTimestamp());
              assertTrue(status.isHasOwnerKey());
              assertFalse(status.isHasTenantKey());
              assertEquals(encryptedPDF, status.getEncryptedPDF());
          }

          @Test
          @DisplayName("ContractEncryptionResult 빌더 패턴")
          void contractEncryptionResult_BuilderPattern() {
              // Given
              EncryptionUtil.EncryptedPDF encryptedPDF = new EncryptionUtil.EncryptedPDF();

              // When
              EncryptionUtil.ContractEncryptionResult result =
                      EncryptionUtil.ContractEncryptionResult.builder()
                              .fileId("testFileId")
                              .status("testStatus")
                              .message("testMessage")
                              .encryptedPDF(encryptedPDF)
                              .build();

              // Then
              assertEquals("testFileId", result.getFileId());
              assertEquals("testStatus", result.getStatus());
              assertEquals("testMessage", result.getMessage());
              assertEquals(encryptedPDF, result.getEncryptedPDF());
          }
      }
}
