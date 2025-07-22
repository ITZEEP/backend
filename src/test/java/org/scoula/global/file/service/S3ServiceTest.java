package org.scoula.global.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scoula.global.common.exception.BusinessException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

/**
 * S3Service 단위 테스트
 *
 * <p>AWS S3 파일 저장소 서비스의 기능을 테스트합니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("S3Service 단위 테스트")
class S3ServiceTest {

      @Mock private S3Client s3Client;

      @Mock private S3Utilities s3Utilities;

      private S3ServiceInterface s3Service;

      private final String bucketName = "test-bucket";

      @BeforeEach
      void setUp() {
          s3Service = new S3ServiceImpl(s3Client);
          ReflectionTestUtils.setField(s3Service, "bucketName", bucketName);

          // S3Utilities Mock 설정 (lenient로 설정하여 불필요한 스터빙 경고 방지)
          lenient().when(s3Client.utilities()).thenReturn(s3Utilities);
      }

      @Test
      @DisplayName("파일 업로드가 성공적으로 수행되는지 확인")
      void uploadFile_WithValidFile_ShouldReturnKey() throws IOException {
          // given
          MockMultipartFile file =
                  new MockMultipartFile("file", "test.txt", "text/plain", "test content".getBytes());

          PutObjectResponse putObjectResponse = PutObjectResponse.builder().build();
          when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                  .thenReturn(putObjectResponse);

          // when
          String result = s3Service.uploadFile(file);

          // then
          assertThat(result).isNotNull();
          assertThat(result).startsWith("uploads/");
          assertThat(result).endsWith(".txt");

          verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
      }

      @Test
      @DisplayName("파일명을 지정한 파일 업로드가 성공적으로 수행되는지 확인")
      void uploadFile_WithSpecifiedFileName_ShouldReturnKey() throws IOException {
          // given
          MockMultipartFile file =
                  new MockMultipartFile(
                          "file", "original.txt", "text/plain", "test content".getBytes());
          String specifiedFileName = "custom-name.txt";

          PutObjectResponse putObjectResponse = PutObjectResponse.builder().build();
          when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                  .thenReturn(putObjectResponse);

          // when
          String result = s3Service.uploadFile(file, specifiedFileName);

          // then
          assertThat(result).isNotNull();
          assertThat(result).startsWith("uploads/");
          assertThat(result).endsWith(".txt");

          verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
      }

      @Test
      @DisplayName("빈 파일 업로드 시 예외가 발생하는지 확인")
      void uploadFile_WithEmptyFile_ShouldThrowException() {
          // given
          MockMultipartFile emptyFile = new MockMultipartFile("file", "", "text/plain", new byte[0]);

          // when & then
          assertThatThrownBy(() -> s3Service.uploadFile(emptyFile))
                  .isInstanceOf(BusinessException.class)
                  .hasMessage("빈 파일은 업로드할 수 없습니다");
      }

      @Test
      @DisplayName("파일명이 null인 파일 업로드 시 예외가 발생하는지 확인")
      void uploadFile_WithNullFileName_ShouldThrowException() {
          // given
          MockMultipartFile fileWithNullName =
                  new MockMultipartFile("file", null, "text/plain", "test content".getBytes());

          // when & then
          assertThatThrownBy(() -> s3Service.uploadFile(fileWithNullName))
                  .isInstanceOf(BusinessException.class)
                  .hasMessage("파일명이 올바르지 않습니다");
      }

      @Test
      @DisplayName("50MB를 초과하는 파일 업로드 시 예외가 발생하는지 확인")
      void uploadFile_WithOversizedFile_ShouldThrowException() {
          // given
          long oversizedFileSize = 51 * 1024 * 1024; // 51MB
          byte[] oversizedContent = new byte[(int) Math.min(oversizedFileSize, Integer.MAX_VALUE)];
          MockMultipartFile oversizedFile =
                  new MockMultipartFile("file", "large.txt", "text/plain", oversizedContent);

          // when & then
          assertThatThrownBy(() -> s3Service.uploadFile(oversizedFile))
                  .isInstanceOf(BusinessException.class)
                  .hasMessage("파일 크기가 50MB를 초과했습니다");
      }

      @Test
      @DisplayName("파일 다운로드가 성공적으로 수행되는지 확인")
      void downloadFile_WithValidKey_ShouldReturnInputStream() {
          // given
          String key = "uploads/test.txt";
          byte[] fileContent = "test content".getBytes();
          InputStream inputStream = new ByteArrayInputStream(fileContent);

          @SuppressWarnings("unchecked")
          ResponseInputStream<GetObjectResponse> responseInputStream =
                  mock(ResponseInputStream.class);
          when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseInputStream);

          // when
          InputStream result = s3Service.downloadFile(key);

          // then
          assertThat(result).isNotNull();
          verify(s3Client).getObject(any(GetObjectRequest.class));
      }

      @Test
      @DisplayName("존재하지 않는 파일 다운로드 시 예외가 발생하는지 확인")
      void downloadFile_WithNonExistentKey_ShouldThrowException() {
          // given
          String key = "uploads/nonexistent.txt";
          when(s3Client.getObject(any(GetObjectRequest.class)))
                  .thenThrow(NoSuchKeyException.builder().build());

          // when & then
          assertThatThrownBy(() -> s3Service.downloadFile(key))
                  .isInstanceOf(BusinessException.class)
                  .hasMessage("파일을 찾을 수 없습니다");
      }

      @Test
      @DisplayName("파일 URL 생성이 성공적으로 수행되는지 확인")
      void getFileUrl_WithValidKey_ShouldReturnUrl() throws Exception {
          // given
          String key = "uploads/test.txt";
          String expectedUrl = "https://test-bucket.s3.amazonaws.com/uploads/test.txt";
          URL url = new URL(expectedUrl);

          when(s3Utilities.getUrl(any(GetUrlRequest.class))).thenReturn(url);

          // when
          String result = s3Service.getFileUrl(key);

          // then
          assertThat(result).isEqualTo(expectedUrl);
          verify(s3Utilities).getUrl(any(GetUrlRequest.class));
      }

      @Test
      @DisplayName("파일 존재 여부 확인이 성공적으로 수행되는지 확인")
      void fileExists_WithExistingFile_ShouldReturnTrue() {
          // given
          String key = "uploads/test.txt";
          HeadObjectResponse headObjectResponse = HeadObjectResponse.builder().build();
          when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(headObjectResponse);

          // when
          boolean result = s3Service.fileExists(key);

          // then
          assertThat(result).isTrue();
          verify(s3Client).headObject(any(HeadObjectRequest.class));
      }

      @Test
      @DisplayName("존재하지 않는 파일 확인 시 false를 반환하는지 확인")
      void fileExists_WithNonExistentFile_ShouldReturnFalse() {
          // given
          String key = "uploads/nonexistent.txt";
          when(s3Client.headObject(any(HeadObjectRequest.class)))
                  .thenThrow(NoSuchKeyException.builder().build());

          // when
          boolean result = s3Service.fileExists(key);

          // then
          assertThat(result).isFalse();
          verify(s3Client).headObject(any(HeadObjectRequest.class));
      }

      @Test
      @DisplayName("파일 삭제가 성공적으로 수행되는지 확인")
      void deleteFile_WithValidKey_ShouldReturnTrue() {
          // given
          String key = "uploads/test.txt";
          DeleteObjectResponse deleteObjectResponse = DeleteObjectResponse.builder().build();
          when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                  .thenReturn(deleteObjectResponse);

          // when
          boolean result = s3Service.deleteFile(key);

          // then
          assertThat(result).isTrue();
          verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
      }

      @Test
      @DisplayName("파일 크기 조회가 성공적으로 수행되는지 확인")
      void getFileSize_WithValidKey_ShouldReturnSize() {
          // given
          String key = "uploads/test.txt";
          long expectedSize = 1024L;
          HeadObjectResponse headObjectResponse =
                  HeadObjectResponse.builder().contentLength(expectedSize).build();
          when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(headObjectResponse);

          // when
          long result = s3Service.getFileSize(key);

          // then
          assertThat(result).isEqualTo(expectedSize);
          verify(s3Client).headObject(any(HeadObjectRequest.class));
      }

      @Test
      @DisplayName("존재하지 않는 파일 크기 조회 시 예외가 발생하는지 확인")
      void getFileSize_WithNonExistentKey_ShouldThrowException() {
          // given
          String key = "uploads/nonexistent.txt";
          when(s3Client.headObject(any(HeadObjectRequest.class)))
                  .thenThrow(NoSuchKeyException.builder().build());

          // when & then
          assertThatThrownBy(() -> s3Service.getFileSize(key))
                  .isInstanceOf(BusinessException.class)
                  .hasMessage("파일을 찾을 수 없습니다");
      }

      @Test
      @DisplayName("빈 파일명으로 파일 업로드 시 예외가 발생하는지 확인")
      void uploadFile_WithEmptySpecifiedFileName_ShouldThrowException() {
          // given
          MockMultipartFile file =
                  new MockMultipartFile(
                          "file", "original.txt", "text/plain", "test content".getBytes());
          String emptyFileName = "";

          // when & then
          assertThatThrownBy(() -> s3Service.uploadFile(file, emptyFileName))
                  .isInstanceOf(BusinessException.class)
                  .hasMessage("파일명이 올바르지 않습니다");
      }

      @Test
      @DisplayName("null 파일명으로 파일 업로드 시 예외가 발생하는지 확인")
      void uploadFile_WithNullSpecifiedFileName_ShouldThrowException() {
          // given
          MockMultipartFile file =
                  new MockMultipartFile(
                          "file", "original.txt", "text/plain", "test content".getBytes());
          String nullFileName = null;

          // when & then
          assertThatThrownBy(() -> s3Service.uploadFile(file, nullFileName))
                  .isInstanceOf(BusinessException.class)
                  .hasMessage("파일명이 올바르지 않습니다");
      }

      @Test
      @DisplayName("확장자가 없는 파일의 파일명 생성이 올바르게 수행되는지 확인")
      void uploadFile_WithFileWithoutExtension_ShouldGenerateValidKey() throws IOException {
          // given
          MockMultipartFile file =
                  new MockMultipartFile("file", "filename", "text/plain", "test content".getBytes());

          PutObjectResponse putObjectResponse = PutObjectResponse.builder().build();
          when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                  .thenReturn(putObjectResponse);

          // when
          String result = s3Service.uploadFile(file);

          // then
          assertThat(result).isNotNull();
          assertThat(result).startsWith("uploads/");
          // 확장자가 없는 파일의 경우 UUID만 생성되어야 함
          assertThat(result.split("/")[1]).matches("[0-9a-f-]{36}");

          verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
      }
}
