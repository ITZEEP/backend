package org.scoula.global.file.controller;

import javax.validation.Valid;

import org.scoula.global.file.dto.S3Dto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/** S3 파일 처리 테스트 컨트롤러 인터페이스 */
@Api(tags = "S3 파일 테스트", description = "S3 파일 업로드/다운로드/삭제 테스트 API")
@RequestMapping("/api/s3")
public interface S3TestController {

      @ApiOperation(value = "파일 업로드", notes = "S3에 파일을 업로드합니다")
      @PostMapping("/upload")
      ResponseEntity<org.scoula.global.common.dto.ApiResponse<S3Dto.UploadResponse>> uploadFile(
              @ApiParam(value = "업로드할 파일", required = true) @RequestParam("file") MultipartFile file);

      @ApiOperation(value = "파일 다운로드", notes = "S3에서 파일을 다운로드합니다")
      @GetMapping("/download/{fileKey}")
      ResponseEntity<byte[]> downloadFile(
              @ApiParam(value = "파일 키", required = true) @PathVariable String fileKey);

      @ApiOperation(value = "파일 정보 조회", notes = "S3 파일 정보를 조회합니다")
      @GetMapping("/info/{fileKey}")
      ResponseEntity<org.scoula.global.common.dto.ApiResponse<S3Dto.FileInfoResponse>> getFileInfo(
              @ApiParam(value = "파일 키", required = true) @PathVariable String fileKey);

      @ApiOperation(value = "파일 삭제", notes = "S3에서 파일을 삭제합니다")
      @DeleteMapping("/delete")
      ResponseEntity<org.scoula.global.common.dto.ApiResponse<S3Dto.DeleteResponse>> deleteFile(
              @Valid @RequestBody S3Dto.DeleteRequest request);

      @ApiOperation(value = "미리 서명된 URL 생성", notes = "파일 다운로드용 미리 서명된 URL을 생성합니다")
      @PostMapping("/presigned-url")
      ResponseEntity<org.scoula.global.common.dto.ApiResponse<S3Dto.PresignedUrlResponse>>
              generatePresignedUrl(@Valid @RequestBody S3Dto.PresignedUrlRequest request);
}
