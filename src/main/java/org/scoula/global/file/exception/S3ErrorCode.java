package org.scoula.global.file.exception;

import org.scoula.global.common.exception.IErrorCode;
import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** S3 파일 처리 관련 에러 코드 */
@Getter
@RequiredArgsConstructor
public enum S3ErrorCode implements IErrorCode {

      // 파일 업로드 관련 오류
      FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S001", "파일 업로드에 실패했습니다"),
      INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "S002", "지원하지 않는 파일 형식입니다"),
      FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "S003", "파일 크기가 제한을 초과했습니다"),
      FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "S004", "파일을 찾을 수 없습니다"),
      FILE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S005", "파일 삭제에 실패했습니다"),

      // S3 접근 관련 오류
      S3_ACCESS_DENIED(HttpStatus.FORBIDDEN, "S006", "S3 접근이 거부되었습니다"),
      S3_BUCKET_NOT_FOUND(HttpStatus.NOT_FOUND, "S007", "S3 버킷을 찾을 수 없습니다"),
      S3_CONNECTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S008", "S3 연결에 실패했습니다"),

      // 파일 검증 관련 오류
      EMPTY_FILE(HttpStatus.BAD_REQUEST, "S009", "빈 파일은 업로드할 수 없습니다"),
      INVALID_FILE_NAME(HttpStatus.BAD_REQUEST, "S010", "파일명이 올바르지 않습니다"),
      FILE_CONTENT_MISMATCH(HttpStatus.BAD_REQUEST, "S011", "파일 내용과 확장자가 일치하지 않습니다");

      private final HttpStatus httpStatus;
      private final String code;
      private final String message;
}
