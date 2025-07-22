package org.scoula.global.file.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scoula.global.common.dto.ApiResponse;
import org.scoula.global.common.exception.BusinessException;
import org.scoula.global.file.dto.S3Dto;
import org.scoula.global.file.exception.S3ErrorCode;
import org.scoula.global.file.service.S3ServiceInterface;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

/**
 * S3TestControllerImpl 단위 테스트
 *
 * <p>S3 파일 저장소 테스트 컨트롤러의 기능을 테스트합니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("S3TestControllerImpl 단위 테스트")
class S3TestControllerImplTest {

      @Mock private S3ServiceInterface s3Service;

      @InjectMocks private S3TestControllerImpl s3TestController;

      @Test
      @DisplayName("파일 업로드 - 성공")
      void uploadFile_ShouldReturnSuccessResponse() {
          // given
          MockMultipartFile file =
                  new MockMultipartFile("file", "test.txt", "text/plain", "test content".getBytes());
          String expectedFileKey = "uploads/test-uuid.txt";
          String expectedFileUrl = "https://test-bucket.s3.amazonaws.com/uploads/test-uuid.txt";

          when(s3Service.uploadFile(file)).thenReturn(expectedFileKey);
          when(s3Service.getFileUrl(expectedFileKey)).thenReturn(expectedFileUrl);

          // when
          ResponseEntity<ApiResponse<S3Dto.UploadResponse>> response =
                  s3TestController.uploadFile(file);

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody().isSuccess()).isTrue();

          S3Dto.UploadResponse uploadResponse = response.getBody().getData();
          assertThat(uploadResponse.getFileKey()).isEqualTo(expectedFileKey);
          assertThat(uploadResponse.getFileUrl()).isEqualTo(expectedFileUrl);
          assertThat(uploadResponse.getOriginalFileName()).isEqualTo("test.txt");
          assertThat(uploadResponse.getFileSize()).isEqualTo(file.getSize());
          assertThat(uploadResponse.getContentType()).isEqualTo("text/plain");
          assertThat(uploadResponse.getUploadedAt()).isNotNull();

          verify(s3Service).uploadFile(file);
          verify(s3Service).getFileUrl(expectedFileKey);
      }

      @Test
      @DisplayName("파일 업로드 - 실패 시 예외 처리")
      void uploadFile_WithException_ShouldReturnErrorResponse() {
          // given
          MockMultipartFile file =
                  new MockMultipartFile("file", "test.txt", "text/plain", "test content".getBytes());

          when(s3Service.uploadFile(file))
                  .thenThrow(new BusinessException(S3ErrorCode.FILE_UPLOAD_FAILED, "업로드 실패"));

          // when
          ResponseEntity<ApiResponse<S3Dto.UploadResponse>> response =
                  s3TestController.uploadFile(file);

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody().isSuccess()).isFalse();
          assertThat(response.getBody().getMessage()).contains("업로드 실패");

          verify(s3Service).uploadFile(file);
      }

      @Test
      @DisplayName("파일 다운로드 - 성공")
      void downloadFile_ShouldReturnFileContent() throws Exception {
          // given
          String fileKey = "uploads/test.txt";
          byte[] fileContent = "test content".getBytes();
          InputStream inputStream = new ByteArrayInputStream(fileContent);
          long fileSize = fileContent.length;

          when(s3Service.downloadFile(fileKey)).thenReturn(inputStream);
          when(s3Service.getFileSize(fileKey)).thenReturn(fileSize);

          // when
          ResponseEntity<byte[]> response = s3TestController.downloadFile(fileKey);

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
          assertThat(response.getBody()).isEqualTo(fileContent);
          assertThat(response.getHeaders().getContentLength()).isEqualTo(fileSize);
          assertThat(response.getHeaders().getContentDisposition().getFilename())
                  .isEqualTo("test.txt");

          verify(s3Service).downloadFile(fileKey);
          verify(s3Service).getFileSize(fileKey);
      }

      @Test
      @DisplayName("파일 정보 조회 - 존재하는 파일")
      void getFileInfo_WithExistingFile_ShouldReturnFileInfo() {
          // given
          String fileKey = "uploads/test.txt";
          String fileUrl = "https://test-bucket.s3.amazonaws.com/uploads/test.txt";
          String presignedUrl = "https://test-bucket.s3.amazonaws.com/uploads/test.txt?signed";

          when(s3Service.fileExists(fileKey)).thenReturn(true);
          when(s3Service.getFileUrl(fileKey)).thenReturn(fileUrl);
          when(s3Service.generatePresignedUrl(fileKey, 60)).thenReturn(presignedUrl);

          // when
          ResponseEntity<ApiResponse<S3Dto.FileInfoResponse>> response =
                  s3TestController.getFileInfo(fileKey);

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody().isSuccess()).isTrue();

          S3Dto.FileInfoResponse fileInfo = response.getBody().getData();
          assertThat(fileInfo.getFileKey()).isEqualTo(fileKey);
          assertThat(fileInfo.getFileUrl()).isEqualTo(fileUrl);
          assertThat(fileInfo.getPresignedUrl()).isEqualTo(presignedUrl);
          assertThat(fileInfo.getExists()).isTrue();
          assertThat(fileInfo.getQueriedAt()).isNotNull();

          verify(s3Service).fileExists(fileKey);
          verify(s3Service).getFileUrl(fileKey);
          verify(s3Service).generatePresignedUrl(fileKey, 60);
      }

      @Test
      @DisplayName("파일 정보 조회 - 존재하지 않는 파일")
      void getFileInfo_WithNonExistentFile_ShouldReturnEmptyInfo() {
          // given
          String fileKey = "uploads/nonexistent.txt";

          when(s3Service.fileExists(fileKey)).thenReturn(false);

          // when
          ResponseEntity<ApiResponse<S3Dto.FileInfoResponse>> response =
                  s3TestController.getFileInfo(fileKey);

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody().isSuccess()).isTrue();

          S3Dto.FileInfoResponse fileInfo = response.getBody().getData();
          assertThat(fileInfo.getFileKey()).isEqualTo(fileKey);
          assertThat(fileInfo.getFileUrl()).isNull();
          assertThat(fileInfo.getPresignedUrl()).isNull();
          assertThat(fileInfo.getExists()).isFalse();
          assertThat(fileInfo.getQueriedAt()).isNotNull();

          verify(s3Service).fileExists(fileKey);
      }

      @Test
      @DisplayName("파일 삭제 - 성공")
      void deleteFile_ShouldReturnSuccessResponse() {
          // given
          S3Dto.DeleteRequest request =
                  S3Dto.DeleteRequest.builder().fileKey("uploads/test.txt").build();

          when(s3Service.deleteFile(request.getFileKey())).thenReturn(true);

          // when
          ResponseEntity<ApiResponse<S3Dto.DeleteResponse>> response =
                  s3TestController.deleteFile(request);

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody().isSuccess()).isTrue();

          S3Dto.DeleteResponse deleteResponse = response.getBody().getData();
          assertThat(deleteResponse.getFileKey()).isEqualTo(request.getFileKey());
          assertThat(deleteResponse.getDeleted()).isTrue();
          assertThat(deleteResponse.getDeletedAt()).isNotNull();

          verify(s3Service).deleteFile(request.getFileKey());
      }

      @Test
      @DisplayName("미리 서명된 URL 생성 - 성공")
      void generatePresignedUrl_ShouldReturnPresignedUrl() {
          // given
          S3Dto.PresignedUrlRequest request =
                  S3Dto.PresignedUrlRequest.builder()
                          .fileKey("uploads/test.txt")
                          .durationMinutes(30)
                          .build();
          String expectedPresignedUrl =
                  "https://test-bucket.s3.amazonaws.com/uploads/test.txt?signed";

          when(s3Service.generatePresignedUrl(request.getFileKey(), 30))
                  .thenReturn(expectedPresignedUrl);

          // when
          ResponseEntity<ApiResponse<S3Dto.PresignedUrlResponse>> response =
                  s3TestController.generatePresignedUrl(request);

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody().isSuccess()).isTrue();

          S3Dto.PresignedUrlResponse presignedUrlResponse = response.getBody().getData();
          assertThat(presignedUrlResponse.getFileKey()).isEqualTo(request.getFileKey());
          assertThat(presignedUrlResponse.getPresignedUrl()).isEqualTo(expectedPresignedUrl);
          assertThat(presignedUrlResponse.getGeneratedAt()).isNotNull();
          assertThat(presignedUrlResponse.getExpiresAt()).isNotNull();

          verify(s3Service).generatePresignedUrl(request.getFileKey(), 30);
      }

      @Test
      @DisplayName("미리 서명된 URL 생성 - 유효 시간 제한 적용")
      void generatePresignedUrl_WithExtremeDuration_ShouldApplyLimits() {
          // given
          S3Dto.PresignedUrlRequest requestTooShort =
                  S3Dto.PresignedUrlRequest.builder()
                          .fileKey("uploads/test.txt")
                          .durationMinutes(0) // 0분 -> 1분으로 제한
                          .build();

          S3Dto.PresignedUrlRequest requestTooLong =
                  S3Dto.PresignedUrlRequest.builder()
                          .fileKey("uploads/test.txt")
                          .durationMinutes(2000) // 2000분 -> 1440분(24시간)으로 제한
                          .build();

          String expectedPresignedUrl =
                  "https://test-bucket.s3.amazonaws.com/uploads/test.txt?signed";

          when(s3Service.generatePresignedUrl(anyString(), anyInt()))
                  .thenReturn(expectedPresignedUrl);

          // when
          ResponseEntity<ApiResponse<S3Dto.PresignedUrlResponse>> responseTooShort =
                  s3TestController.generatePresignedUrl(requestTooShort);
          ResponseEntity<ApiResponse<S3Dto.PresignedUrlResponse>> responseTooLong =
                  s3TestController.generatePresignedUrl(requestTooLong);

          // then
          assertThat(responseTooShort.getStatusCode()).isEqualTo(HttpStatus.OK);
          assertThat(responseTooLong.getStatusCode()).isEqualTo(HttpStatus.OK);

          // 최소 1분으로 제한되었는지 확인
          verify(s3Service).generatePresignedUrl(eq("uploads/test.txt"), eq(1));
          // 최대 1440분으로 제한되었는지 확인
          verify(s3Service).generatePresignedUrl(eq("uploads/test.txt"), eq(1440));
      }

      @Test
      @DisplayName("파일 삭제 - 실패 시 예외 처리")
      void deleteFile_WithException_ShouldReturnErrorResponse() {
          // given
          S3Dto.DeleteRequest request =
                  S3Dto.DeleteRequest.builder().fileKey("uploads/test.txt").build();

          when(s3Service.deleteFile(request.getFileKey()))
                  .thenThrow(new BusinessException(S3ErrorCode.FILE_NOT_FOUND, "파일을 찾을 수 없습니다"));

          // when
          ResponseEntity<ApiResponse<S3Dto.DeleteResponse>> response =
                  s3TestController.deleteFile(request);

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody().isSuccess()).isFalse();
          assertThat(response.getBody().getMessage()).contains("파일을 찾을 수 없습니다");

          verify(s3Service).deleteFile(request.getFileKey());
      }

      @Test
      @DisplayName("미리 서명된 URL 생성 - 실패 시 예외 처리")
      void generatePresignedUrl_WithException_ShouldReturnErrorResponse() {
          // given
          S3Dto.PresignedUrlRequest request =
                  S3Dto.PresignedUrlRequest.builder()
                          .fileKey("uploads/test.txt")
                          .durationMinutes(30)
                          .build();

          when(s3Service.generatePresignedUrl(request.getFileKey(), 30))
                  .thenThrow(new BusinessException(S3ErrorCode.FILE_NOT_FOUND, "파일을 찾을 수 없습니다"));

          // when
          ResponseEntity<ApiResponse<S3Dto.PresignedUrlResponse>> response =
                  s3TestController.generatePresignedUrl(request);

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody().isSuccess()).isFalse();
          assertThat(response.getBody().getMessage()).contains("파일을 찾을 수 없습니다");

          verify(s3Service).generatePresignedUrl(request.getFileKey(), 30);
      }
}
