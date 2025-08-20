package org.scoula.global.common.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@DisplayName("MultipartFileUtils 테스트")
class MultipartFileUtilsTest {

      @TempDir Path tempDir;

      private File testFile;
      private File[] filesToCleanup = new File[10];
      private int cleanupIndex = 0;

      private void addForCleanup(File file) {
          if (cleanupIndex < filesToCleanup.length) {
              filesToCleanup[cleanupIndex++] = file;
          }
      }

      @AfterEach
      void cleanup() {
          // 테스트 중 생성된 임시 파일들 정리
          for (int i = 0; i < cleanupIndex; i++) {
              if (filesToCleanup[i] != null && filesToCleanup[i].exists()) {
                  filesToCleanup[i].delete();
              }
          }
          cleanupIndex = 0;
      }

      @Nested
      @DisplayName("fromFile 메서드 테스트")
      class FromFileTest {

          @Test
          @DisplayName("File을 MultipartFile로 변환 - 기본 메서드")
          void fromFile_WithFileOnly() throws IOException {
              // Given
              testFile = createTestFile("test.txt", "Hello World");
              addForCleanup(testFile);

              // When
              MultipartFile multipartFile = MultipartFileUtils.fromFile(testFile);

              // Then
              assertNotNull(multipartFile);
              assertEquals("test.txt", multipartFile.getOriginalFilename());
              assertEquals(testFile.length(), multipartFile.getSize());
              assertFalse(multipartFile.isEmpty());
              assertArrayEquals("Hello World".getBytes(), multipartFile.getBytes());
          }

          @Test
          @DisplayName("File을 MultipartFile로 변환 - 파일명과 Content-Type 지정")
          void fromFile_WithCustomValues() throws IOException {
              // Given
              testFile = createTestFile("original.txt", "Test content");
              addForCleanup(testFile);
              String customFilename = "custom.txt";
              String customContentType = "text/plain";

              // When
              MultipartFile multipartFile =
                      MultipartFileUtils.fromFile(testFile, customFilename, customContentType);

              // Then
              assertNotNull(multipartFile);
              assertEquals(customFilename, multipartFile.getOriginalFilename());
              assertEquals(customContentType, multipartFile.getContentType());
              assertEquals(testFile.length(), multipartFile.getSize());
              assertArrayEquals("Test content".getBytes(), multipartFile.getBytes());
          }

          @Test
          @DisplayName("null 파일로 예외 발생")
          void fromFile_WithNullFile() {
              // When & Then
              NullPointerException exception =
                      assertThrows(
                              NullPointerException.class, () -> MultipartFileUtils.fromFile(null));
              assertEquals(
                      "Cannot invoke \"java.io.File.getName()\" because \"file\" is null",
                      exception.getMessage());
          }

          @Test
          @DisplayName("빈 파일 처리")
          void fromFile_WithEmptyFile() throws IOException {
              // Given
              testFile = createTestFile("empty.txt", "");
              addForCleanup(testFile);

              // When
              MultipartFile multipartFile = MultipartFileUtils.fromFile(testFile);

              // Then
              assertNotNull(multipartFile);
              assertEquals(0, multipartFile.getSize());
              assertTrue(multipartFile.isEmpty());
              assertEquals(0, multipartFile.getBytes().length);
          }
      }

      @Nested
      @DisplayName("toTempFile 메서드 테스트")
      class ToTempFileTest {

          @Test
          @DisplayName("MultipartFile을 임시 파일로 변환")
          void toTempFile_Success() throws IOException {
              // Given
              String content = "MultipartFile to TempFile test";
              MockMultipartFile multipartFile =
                      new MockMultipartFile("file", "test.txt", "text/plain", content.getBytes());

              // When
              File tempFile = MultipartFileUtils.toTempFile(multipartFile, "test_", ".txt");
              addForCleanup(tempFile);

              // Then
              assertNotNull(tempFile);
              assertTrue(tempFile.exists());
              assertTrue(tempFile.getName().startsWith("test_"));
              assertTrue(tempFile.getName().endsWith(".txt"));
              assertEquals(content, Files.readString(tempFile.toPath()));
          }

          @Test
          @DisplayName("null MultipartFile로 예외 발생")
          void toTempFile_WithNullMultipart() {
              // When & Then
              NullPointerException exception =
                      assertThrows(
                              NullPointerException.class,
                              () -> MultipartFileUtils.toTempFile(null, "prefix", ".txt"));
              assertTrue(exception.getMessage().contains("multipart must not be null"));
          }

          @Test
          @DisplayName("큰 파일 처리")
          void toTempFile_LargeFile() throws IOException {
              // Given
              byte[] largeContent = new byte[1024 * 1024]; // 1MB
              Arrays.fill(largeContent, (byte) 'A');
              MockMultipartFile multipartFile =
                      new MockMultipartFile(
                              "file", "large.bin", "application/octet-stream", largeContent);

              // When
              File tempFile = MultipartFileUtils.toTempFile(multipartFile, "large_", ".bin");
              addForCleanup(tempFile);

              // Then
              assertNotNull(tempFile);
              assertTrue(tempFile.exists());
              assertEquals(largeContent.length, tempFile.length());
          }
      }

      @Nested
      @DisplayName("inputStreamToTempFile 메서드 테스트")
      class InputStreamToTempFileTest {

          @Test
          @DisplayName("InputStream을 임시 파일로 변환 - 기본 메서드")
          void inputStreamToTempFile_DefaultMethod() throws IOException {
              // Given
              String content = "InputStream to TempFile test";
              InputStream inputStream = new ByteArrayInputStream(content.getBytes());

              // When
              File tempFile = MultipartFileUtils.inputStreamToTempFile(inputStream);
              addForCleanup(tempFile);

              // Then
              assertNotNull(tempFile);
              assertTrue(tempFile.exists());
              assertTrue(tempFile.getName().startsWith("contract_"));
              assertTrue(tempFile.getName().endsWith(".pdf"));
              assertEquals(content, Files.readString(tempFile.toPath()));
          }

          @Test
          @DisplayName("InputStream을 임시 파일로 변환 - prefix/suffix 지정")
          void inputStreamToTempFile_WithPrefixSuffix() throws IOException {
              // Given
              String content = "Custom prefix/suffix test";
              InputStream inputStream = new ByteArrayInputStream(content.getBytes());

              // When
              File tempFile =
                      MultipartFileUtils.inputStreamToTempFile(inputStream, "custom_", ".dat");
              addForCleanup(tempFile);

              // Then
              assertNotNull(tempFile);
              assertTrue(tempFile.exists());
              assertTrue(tempFile.getName().startsWith("custom_"));
              assertTrue(tempFile.getName().endsWith(".dat"));
              assertEquals(content, Files.readString(tempFile.toPath()));
          }

          @Test
          @DisplayName("null 또는 blank prefix/suffix 처리")
          void inputStreamToTempFile_WithNullBlankValues() throws IOException {
              // Given
              String content = "Null/blank values test";
              InputStream inputStream1 = new ByteArrayInputStream(content.getBytes());
              InputStream inputStream2 = new ByteArrayInputStream(content.getBytes());
              InputStream inputStream3 = new ByteArrayInputStream(content.getBytes());

              // When
              File tempFile1 = MultipartFileUtils.inputStreamToTempFile(inputStream1, null, null);
              File tempFile2 = MultipartFileUtils.inputStreamToTempFile(inputStream2, "", "");
              File tempFile3 = MultipartFileUtils.inputStreamToTempFile(inputStream3, "  ", "  ");
              addForCleanup(tempFile1);
              addForCleanup(tempFile2);
              addForCleanup(tempFile3);

              // Then
              assertTrue(tempFile1.getName().startsWith("contract_"));
              assertTrue(tempFile1.getName().endsWith(".pdf"));
              assertTrue(tempFile2.getName().startsWith("contract_"));
              assertTrue(tempFile2.getName().endsWith(".pdf"));
              assertTrue(tempFile3.getName().startsWith("contract_"));
              assertTrue(tempFile3.getName().endsWith(".pdf"));
          }

          @Test
          @DisplayName("null InputStream으로 예외 발생")
          void inputStreamToTempFile_WithNullInputStream() {
              // When & Then
              NullPointerException exception =
                      assertThrows(
                              NullPointerException.class,
                              () -> MultipartFileUtils.inputStreamToTempFile(null));
              assertTrue(exception.getMessage().contains("inputStream must not be null"));
          }
      }

      @Nested
      @DisplayName("inputStreamToMultipartFile 메서드 테스트")
      class InputStreamToMultipartFileTest {

          @Test
          @DisplayName("InputStream을 MultipartFile로 변환 - 기본 메서드")
          void inputStreamToMultipartFile_DefaultMethod() throws IOException {
              // Given
              String content = "InputStream to MultipartFile test";
              InputStream inputStream = new ByteArrayInputStream(content.getBytes());

              // When
              MultipartFile multipartFile =
                      MultipartFileUtils.inputStreamToMultipartFile(inputStream);

              // Then
              assertNotNull(multipartFile);
              assertEquals("file", multipartFile.getOriginalFilename());
              assertEquals("application/octet-stream", multipartFile.getContentType());
              assertEquals(content.length(), multipartFile.getSize());
              assertArrayEquals(content.getBytes(), multipartFile.getBytes());
          }

          @Test
          @DisplayName("InputStream을 MultipartFile로 변환 - 파일명과 Content-Type 지정")
          void inputStreamToMultipartFile_WithCustomValues() throws IOException {
              // Given
              String content = "Custom values test";
              InputStream inputStream = new ByteArrayInputStream(content.getBytes());

              // When
              MultipartFile multipartFile =
                      MultipartFileUtils.inputStreamToMultipartFile(
                              inputStream, "custom.txt", "text/plain");

              // Then
              assertNotNull(multipartFile);
              assertEquals("custom.txt", multipartFile.getOriginalFilename());
              assertEquals("text/plain", multipartFile.getContentType());
              assertArrayEquals(content.getBytes(), multipartFile.getBytes());
          }

          @Test
          @DisplayName("null 또는 blank 값들 처리")
          void inputStreamToMultipartFile_WithNullBlankValues() throws IOException {
              // Given
              String content = "Null/blank values test";
              InputStream inputStream1 = new ByteArrayInputStream(content.getBytes());
              InputStream inputStream2 = new ByteArrayInputStream(content.getBytes());
              InputStream inputStream3 = new ByteArrayInputStream(content.getBytes());

              // When
              MultipartFile multipartFile1 =
                      MultipartFileUtils.inputStreamToMultipartFile(inputStream1, null, null);
              MultipartFile multipartFile2 =
                      MultipartFileUtils.inputStreamToMultipartFile(inputStream2, "", "");
              MultipartFile multipartFile3 =
                      MultipartFileUtils.inputStreamToMultipartFile(inputStream3, "  ", "  ");

              // Then
              assertEquals("file", multipartFile1.getOriginalFilename());
              assertEquals("application/octet-stream", multipartFile1.getContentType());
              assertEquals("file", multipartFile2.getOriginalFilename());
              assertEquals("application/octet-stream", multipartFile2.getContentType());
              assertEquals("file", multipartFile3.getOriginalFilename());
              assertEquals("application/octet-stream", multipartFile3.getContentType());
          }

          @Test
          @DisplayName("null InputStream으로 예외 발생")
          void inputStreamToMultipartFile_WithNullInputStream() {
              // When & Then
              NullPointerException exception =
                      assertThrows(
                              NullPointerException.class,
                              () -> MultipartFileUtils.inputStreamToMultipartFile(null));
              assertTrue(exception.getMessage().contains("inputStream must not be null"));
          }
      }

      @Nested
      @DisplayName("바이트 변환 메서드 테스트")
      class ByteConversionTest {

          @Test
          @DisplayName("File을 byte[]로 변환")
          void fileToBytes_Success() throws IOException {
              // Given
              String content = "File to bytes test";
              testFile = createTestFile("test.txt", content);
              addForCleanup(testFile);

              // When
              byte[] bytes = MultipartFileUtils.fileToBytes(testFile);

              // Then
              assertNotNull(bytes);
              assertArrayEquals(content.getBytes(), bytes);
          }

          @Test
          @DisplayName("null File로 예외 발생")
          void fileToBytes_WithNullFile() {
              // When & Then
              NullPointerException exception =
                      assertThrows(
                              NullPointerException.class, () -> MultipartFileUtils.fileToBytes(null));
              assertTrue(exception.getMessage().contains("file must not be null"));
          }

          @Test
          @DisplayName("MultipartFile을 byte[]로 변환")
          void multipartToBytes_Success() throws IOException {
              // Given
              String content = "MultipartFile to bytes test";
              MockMultipartFile multipartFile =
                      new MockMultipartFile("file", "test.txt", "text/plain", content.getBytes());

              // When
              byte[] bytes = MultipartFileUtils.multipartToBytes(multipartFile);

              // Then
              assertNotNull(bytes);
              assertArrayEquals(content.getBytes(), bytes);
          }

          @Test
          @DisplayName("null MultipartFile로 예외 발생")
          void multipartToBytes_WithNullMultipart() {
              // When & Then
              NullPointerException exception =
                      assertThrows(
                              NullPointerException.class,
                              () -> MultipartFileUtils.multipartToBytes(null));
              assertTrue(exception.getMessage().contains("multipart must not be null"));
          }

          @Test
          @DisplayName("InputStream을 byte[]로 변환")
          void inputStreamToBytes_Success() throws IOException {
              // Given
              String content = "InputStream to bytes test";
              InputStream inputStream = new ByteArrayInputStream(content.getBytes());

              // When
              byte[] bytes = MultipartFileUtils.inputStreamToBytes(inputStream);

              // Then
              assertNotNull(bytes);
              assertArrayEquals(content.getBytes(), bytes);
          }

          @Test
          @DisplayName("큰 InputStream을 byte[]로 변환")
          void inputStreamToBytes_LargeStream() throws IOException {
              // Given
              byte[] largeContent = new byte[16384]; // 16KB (8192 버퍼보다 큰 크기)
              Arrays.fill(largeContent, (byte) 'X');
              InputStream inputStream = new ByteArrayInputStream(largeContent);

              // When
              byte[] bytes = MultipartFileUtils.inputStreamToBytes(inputStream);

              // Then
              assertNotNull(bytes);
              assertArrayEquals(largeContent, bytes);
          }

          @Test
          @DisplayName("null InputStream으로 예외 발생")
          void inputStreamToBytes_WithNullInputStream() {
              // When & Then
              NullPointerException exception =
                      assertThrows(
                              NullPointerException.class,
                              () -> MultipartFileUtils.inputStreamToBytes(null));
              assertTrue(exception.getMessage().contains("inputStream must not be null"));
          }
      }

      @Nested
      @DisplayName("bytesToTempFile 메서드 테스트")
      class BytesToTempFileTest {

          @Test
          @DisplayName("byte[]를 임시 파일로 변환")
          void bytesToTempFile_Success() throws IOException {
              // Given
              String content = "Bytes to temp file test";
              byte[] bytes = content.getBytes();

              // When
              File tempFile = MultipartFileUtils.bytesToTempFile(bytes, "bytes_", ".txt");
              addForCleanup(tempFile);

              // Then
              assertNotNull(tempFile);
              assertTrue(tempFile.exists());
              assertTrue(tempFile.getName().startsWith("bytes_"));
              assertTrue(tempFile.getName().endsWith(".txt"));
              assertEquals(content, Files.readString(tempFile.toPath()));
          }

          @Test
          @DisplayName("null 또는 blank prefix/suffix 처리")
          void bytesToTempFile_WithNullBlankValues() throws IOException {
              // Given
              byte[] bytes = "test".getBytes();

              // When
              File tempFile1 = MultipartFileUtils.bytesToTempFile(bytes, null, null);
              File tempFile2 = MultipartFileUtils.bytesToTempFile(bytes, "", "");
              File tempFile3 = MultipartFileUtils.bytesToTempFile(bytes, "  ", "  ");
              addForCleanup(tempFile1);
              addForCleanup(tempFile2);
              addForCleanup(tempFile3);

              // Then
              assertTrue(tempFile1.getName().startsWith("contract_"));
              assertTrue(tempFile1.getName().endsWith(".bin"));
              assertTrue(tempFile2.getName().startsWith("contract_"));
              assertTrue(tempFile2.getName().endsWith(".bin"));
              assertTrue(tempFile3.getName().startsWith("contract_"));
              assertTrue(tempFile3.getName().endsWith(".bin"));
          }

          @Test
          @DisplayName("null bytes로 예외 발생")
          void bytesToTempFile_WithNullBytes() {
              // When & Then
              NullPointerException exception =
                      assertThrows(
                              NullPointerException.class,
                              () -> MultipartFileUtils.bytesToTempFile(null, "prefix", ".txt"));
              assertTrue(exception.getMessage().contains("bytes must not be null"));
          }

          @Test
          @DisplayName("빈 byte[] 처리")
          void bytesToTempFile_WithEmptyBytes() throws IOException {
              // Given
              byte[] emptyBytes = new byte[0];

              // When
              File tempFile = MultipartFileUtils.bytesToTempFile(emptyBytes, "empty_", ".dat");
              addForCleanup(tempFile);

              // Then
              assertNotNull(tempFile);
              assertTrue(tempFile.exists());
              assertEquals(0, tempFile.length());
          }
      }

      @Nested
      @DisplayName("FileMultipartFile 구현체 테스트")
      class FileMultipartFileTest {

          @Test
          @DisplayName("FileMultipartFile의 모든 메서드 동작 확인")
          void fileMultipartFile_AllMethods() throws IOException {
              // Given
              String content = "FileMultipartFile test content";
              testFile = createTestFile("test.txt", content);
              addForCleanup(testFile);

              MultipartFile multipartFile =
                      MultipartFileUtils.fromFile(testFile, "custom.txt", "text/plain");

              // When & Then
              assertEquals("file", multipartFile.getName());
              assertEquals("custom.txt", multipartFile.getOriginalFilename());
              assertEquals("text/plain", multipartFile.getContentType());
              assertFalse(multipartFile.isEmpty());
              assertEquals(content.length(), multipartFile.getSize());
              assertArrayEquals(content.getBytes(), multipartFile.getBytes());

              // InputStream 테스트
              try (InputStream inputStream = multipartFile.getInputStream()) {
                  byte[] readBytes = inputStream.readAllBytes();
                  assertArrayEquals(content.getBytes(), readBytes);
              }

              // transferTo 테스트
              File destFile = tempDir.resolve("transferred.txt").toFile();
              multipartFile.transferTo(destFile);
              addForCleanup(destFile);

              assertTrue(destFile.exists());
              assertEquals(content, Files.readString(destFile.toPath()));
          }

          @Test
          @DisplayName("빈 파일의 isEmpty() 동작")
          void fileMultipartFile_EmptyFile() throws IOException {
              // Given
              testFile = createTestFile("empty.txt", "");
              addForCleanup(testFile);

              // When
              MultipartFile multipartFile = MultipartFileUtils.fromFile(testFile);

              // Then
              assertTrue(multipartFile.isEmpty());
              assertEquals(0, multipartFile.getSize());
              assertEquals(0, multipartFile.getBytes().length);
          }
      }

      @Nested
      @DisplayName("통합 시나리오 테스트")
      class IntegrationTest {

          @Test
          @DisplayName("파일 → MultipartFile → 임시파일 → byte[] 변환 체인")
          void fileConversionChain() throws IOException {
              // Given
              String originalContent = "Integration test content 한글도 포함";
              testFile = createTestFile("original.txt", originalContent);
              addForCleanup(testFile);

              // File → MultipartFile
              MultipartFile multipartFile =
                      MultipartFileUtils.fromFile(testFile, "converted.txt", "text/plain");

              // MultipartFile → 임시파일
              File tempFile = MultipartFileUtils.toTempFile(multipartFile, "temp_", ".txt");
              addForCleanup(tempFile);

              // 임시파일 → byte[]
              byte[] bytes = MultipartFileUtils.fileToBytes(tempFile);

              // Then
              assertEquals(originalContent, new String(bytes));
              assertEquals(originalContent.getBytes().length, bytes.length);
              assertTrue(tempFile.exists());
              assertTrue(tempFile.getName().startsWith("temp_"));
              assertTrue(tempFile.getName().endsWith(".txt"));
          }

          @Test
          @DisplayName("InputStream → MultipartFile → File 변환 체인")
          void streamConversionChain() throws IOException {
              // Given
              String originalContent = "Stream conversion test 🚀 emoji test";
              InputStream inputStream = new ByteArrayInputStream(originalContent.getBytes());

              // InputStream → MultipartFile
              MultipartFile multipartFile =
                      MultipartFileUtils.inputStreamToMultipartFile(
                              inputStream, "stream.txt", "text/plain");

              // MultipartFile → 임시파일
              File tempFile = MultipartFileUtils.toTempFile(multipartFile, "stream_", ".txt");
              addForCleanup(tempFile);

              // Then
              assertEquals("stream.txt", multipartFile.getOriginalFilename());
              assertEquals("text/plain", multipartFile.getContentType());
              assertEquals(originalContent, Files.readString(tempFile.toPath()));
          }

          @Test
          @DisplayName("바이너리 데이터 처리")
          void binaryDataProcessing() throws IOException {
              // Given
              byte[] binaryData = new byte[256];
              for (int i = 0; i < 256; i++) {
                  binaryData[i] = (byte) i;
              }

              // byte[] → 임시파일
              File tempFile = MultipartFileUtils.bytesToTempFile(binaryData, "binary_", ".bin");
              addForCleanup(tempFile);

              // 임시파일 → MultipartFile
              MultipartFile multipartFile =
                      MultipartFileUtils.fromFile(tempFile, "binary.bin", "application/octet-stream");

              // MultipartFile → byte[]
              byte[] convertedBytes = MultipartFileUtils.multipartToBytes(multipartFile);

              // Then
              assertArrayEquals(binaryData, convertedBytes);
              assertEquals("binary.bin", multipartFile.getOriginalFilename());
              assertEquals("application/octet-stream", multipartFile.getContentType());
          }
      }

      // 테스트용 헬퍼 메서드
      private File createTestFile(String filename, String content) throws IOException {
          Path filePath = tempDir.resolve(filename);
          Files.writeString(filePath, content);
          return filePath.toFile();
      }
}
