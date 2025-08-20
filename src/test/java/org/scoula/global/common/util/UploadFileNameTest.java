package org.scoula.global.common.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.scoula.global.common.exception.BusinessException;

@DisplayName("UploadFileName 테스트")
class UploadFileNameTest {

      private static final Pattern TIMESTAMP_PATTERN = Pattern.compile(".*_\\d{8}_\\d{6}\\..*");
      private static final Pattern UUID_PATTERN =
              Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\..*");

      @Nested
      @DisplayName("getUniqueName 메서드 테스트")
      class GetUniqueNameTest {

          @Test
          @DisplayName("정상적인 파일명 처리")
          void getUniqueName_ValidFilename() {
              // Given
              String filename = "test.jpg";

              // When
              String result = UploadFileName.getUniqueName(filename);

              // Then
              assertNotNull(result);
              assertTrue(result.startsWith("test_"));
              assertTrue(result.endsWith(".jpg"));
              assertTrue(TIMESTAMP_PATTERN.matcher(result).matches());
          }

          @Test
          @DisplayName("한글 파일명 처리")
          void getUniqueName_KoreanFilename() {
              // Given
              String filename = "테스트파일.pdf";

              // When
              String result = UploadFileName.getUniqueName(filename);

              // Then
              assertNotNull(result);
              assertTrue(result.startsWith("테스트파일_"));
              assertTrue(result.endsWith(".pdf"));
          }

          @Test
          @DisplayName("여러 점이 있는 파일명 처리")
          void getUniqueName_MultipleDotsFilename() {
              // Given
              String filename = "my.test.file.txt";

              // When
              String result = UploadFileName.getUniqueName(filename);

              // Then
              assertNotNull(result);
              assertTrue(result.startsWith("my.test.file_"));
              assertTrue(result.endsWith(".txt"));
          }

          @ParameterizedTest
          @NullAndEmptySource
          @ValueSource(strings = {"  ", "\t", "\n"})
          @DisplayName("유효하지 않은 파일명으로 예외 발생")
          void getUniqueName_InvalidFilename(String filename) {
              // When & Then
              assertThrows(BusinessException.class, () -> UploadFileName.getUniqueName(filename));
          }

          @Test
          @DisplayName("확장자가 없는 파일명으로 예외 발생")
          void getUniqueName_NoExtension() {
              // Given
              String filename = "noextension";

              // When & Then
              BusinessException exception =
                      assertThrows(
                              BusinessException.class, () -> UploadFileName.getUniqueName(filename));
              assertTrue(exception.getMessage().contains("extension"));
          }

          @Test
          @DisplayName("점으로 끝나는 파일명으로 예외 발생")
          void getUniqueName_EndsWithDot() {
              // Given
              String filename = "test.";

              // When & Then
              assertThrows(BusinessException.class, () -> UploadFileName.getUniqueName(filename));
          }
      }

      @Nested
      @DisplayName("getUuidBasedName 메서드 테스트")
      class GetUuidBasedNameTest {

          @Test
          @DisplayName("정상적인 파일명 처리")
          void getUuidBasedName_ValidFilename() {
              // Given
              String filename = "document.docx";

              // When
              String result = UploadFileName.getUuidBasedName(filename);

              // Then
              assertNotNull(result);
              assertTrue(result.endsWith(".docx"));
              assertTrue(UUID_PATTERN.matcher(result).matches());
          }

          @Test
          @DisplayName("대문자 확장자를 소문자로 변환")
          void getUuidBasedName_UppercaseExtension() {
              // Given
              String filename = "IMAGE.PNG";

              // When
              String result = UploadFileName.getUuidBasedName(filename);

              // Then
              assertNotNull(result);
              assertTrue(result.endsWith(".png"));
              assertFalse(result.endsWith(".PNG"));
          }

          @Test
          @DisplayName("동일 파일명에 대해 매번 다른 UUID 생성")
          void getUuidBasedName_UniqueEveryTime() {
              // Given
              String filename = "test.txt";

              // When
              String result1 = UploadFileName.getUuidBasedName(filename);
              String result2 = UploadFileName.getUuidBasedName(filename);

              // Then
              assertNotEquals(result1, result2);
          }

          @ParameterizedTest
          @NullAndEmptySource
          @DisplayName("유효하지 않은 파일명으로 예외 발생")
          void getUuidBasedName_InvalidFilename(String filename) {
              // When & Then
              assertThrows(BusinessException.class, () -> UploadFileName.getUuidBasedName(filename));
          }
      }

      @Nested
      @DisplayName("extractFileExtension 메서드 테스트")
      class ExtractFileExtensionTest {

          @ParameterizedTest
          @CsvSource({
              "file.txt, txt",
              "image.jpg, jpg",
              "document.PDF, pdf",
              "archive.tar.gz, gz",
              "file.with.dots.xml, xml"
          })
          @DisplayName("다양한 파일명에서 확장자 추출")
          void extractFileExtension_VariousFilenames(String filename, String expectedExtension) {
              // When
              String result = UploadFileName.extractFileExtension(filename);

              // Then
              assertEquals(expectedExtension, result);
          }

          @Test
          @DisplayName("확장자가 없는 파일명으로 예외 발생")
          void extractFileExtension_NoExtension() {
              // Given
              String filename = "noextension";

              // When & Then
              assertThrows(
                      BusinessException.class, () -> UploadFileName.extractFileExtension(filename));
          }

          @Test
          @DisplayName("점으로 끝나는 파일명으로 예외 발생")
          void extractFileExtension_EndsWithDot() {
              // Given
              String filename = "file.";

              // When & Then
              assertThrows(
                      BusinessException.class, () -> UploadFileName.extractFileExtension(filename));
          }

          @ParameterizedTest
          @NullAndEmptySource
          @DisplayName("null 또는 빈 파일명으로 예외 발생")
          void extractFileExtension_NullOrEmpty(String filename) {
              // When & Then
              assertThrows(
                      BusinessException.class, () -> UploadFileName.extractFileExtension(filename));
          }
      }

      @Nested
      @DisplayName("extractFileNameWithoutExtension 메서드 테스트")
      class ExtractFileNameWithoutExtensionTest {

          @ParameterizedTest
          @CsvSource({
              "file.txt, file",
              "image.jpg, image",
              "my.document.pdf, my.document",
              "archive.tar.gz, archive.tar"
          })
          @DisplayName("다양한 파일명에서 확장자 제거")
          void extractFileNameWithoutExtension_VariousFilenames(
                  String filename, String expectedName) {
              // When
              String result = UploadFileName.extractFileNameWithoutExtension(filename);

              // Then
              assertEquals(expectedName, result);
          }

          @Test
          @DisplayName("확장자가 없는 파일명 그대로 반환")
          void extractFileNameWithoutExtension_NoExtension() {
              // Given
              String filename = "noextension";

              // When
              String result = UploadFileName.extractFileNameWithoutExtension(filename);

              // Then
              assertEquals(filename, result);
          }

          @Test
          @DisplayName("점으로 시작하는 파일명 처리")
          void extractFileNameWithoutExtension_StartsWithDot() {
              // Given
              String filename = ".hiddenfile.txt";

              // When
              String result = UploadFileName.extractFileNameWithoutExtension(filename);

              // Then
              assertEquals(".hiddenfile", result);
          }

          @ParameterizedTest
          @NullAndEmptySource
          @DisplayName("null 또는 빈 파일명으로 예외 발생")
          void extractFileNameWithoutExtension_NullOrEmpty(String filename) {
              // When & Then
              assertThrows(
                      BusinessException.class,
                      () -> UploadFileName.extractFileNameWithoutExtension(filename));
          }
      }

      @Nested
      @DisplayName("파일명 유효성 검증 테스트")
      class FilenameValidationTest {

          @ParameterizedTest
          @ValueSource(
                  strings = {
                      "file<name>.txt",
                      "file>name.txt",
                      "file:name.txt",
                      "file\"name.txt",
                      "file/name.txt",
                      "file|name.txt",
                      "file?name.txt",
                      "file*name.txt"
                  })
          @DisplayName("유효하지 않은 문자가 포함된 파일명으로 예외 발생")
          void validateFilename_InvalidCharacters(String filename) {
              // When & Then
              BusinessException exception =
                      assertThrows(
                              BusinessException.class, () -> UploadFileName.getUniqueName(filename));
              assertTrue(exception.getMessage().contains("invalid character"));
          }

          @Test
          @DisplayName("공백만 있는 파일명으로 예외 발생")
          void validateFilename_OnlySpaces() {
              // Given
              String filename = "   ";

              // When & Then
              assertThrows(BusinessException.class, () -> UploadFileName.getUniqueName(filename));
          }

          @Test
          @DisplayName("null 파일명으로 예외 발생")
          void validateFilename_Null() {
              // Given
              String filename = null;

              // When & Then
              BusinessException exception =
                      assertThrows(
                              BusinessException.class, () -> UploadFileName.getUniqueName(filename));
              assertTrue(exception.getMessage().contains("cannot be null"));
          }
      }

      @Nested
      @DisplayName("특수 케이스 테스트")
      class SpecialCasesTest {

          @Test
          @DisplayName("매우 긴 파일명 처리")
          void handleVeryLongFilename() {
              // Given
              String longName = "a".repeat(200) + ".txt";

              // When
              String result = UploadFileName.getUniqueName(longName);

              // Then
              assertNotNull(result);
              assertTrue(result.contains("a".repeat(200)));
              assertTrue(result.endsWith(".txt"));
          }

          @Test
          @DisplayName("특수문자가 포함된 유효한 파일명")
          void handleSpecialCharactersValid() {
              // Given
              String filename = "file-name_123 (copy).txt";

              // When
              String result = UploadFileName.getUniqueName(filename);

              // Then
              assertNotNull(result);
              assertTrue(result.startsWith("file-name_123 (copy)_"));
              assertTrue(result.endsWith(".txt"));
          }

          @Test
          @DisplayName("타임스탬프가 고유함을 보장")
          void ensureTimestampUniqueness() throws InterruptedException {
              // Given
              String filename = "test.txt";

              // When
              String result1 = UploadFileName.getUniqueName(filename);
              Thread.sleep(1001); // 1초 대기
              String result2 = UploadFileName.getUniqueName(filename);

              // Then
              assertNotEquals(result1, result2);
          }
      }
}
