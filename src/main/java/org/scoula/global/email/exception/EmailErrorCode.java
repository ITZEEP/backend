package org.scoula.global.email.exception;

import org.scoula.global.common.exception.IErrorCode;
import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 이메일 관련 에러 코드 */
@Getter
@RequiredArgsConstructor
public enum EmailErrorCode implements IErrorCode {

      // 이메일 전송 관련 오류
      EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "E001", "이메일 전송에 실패했습니다"),
      SMTP_CONNECTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "E002", "SMTP 서버 연결에 실패했습니다"),
      INVALID_EMAIL_ADDRESS(HttpStatus.BAD_REQUEST, "E003", "올바르지 않은 이메일 주소입니다"),
      EMAIL_TEMPLATE_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "E004", "이메일 템플릿을 찾을 수 없습니다"),

      // 첨부파일 관련 오류
      ATTACHMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "E005", "첨부파일을 찾을 수 없습니다"),
      ATTACHMENT_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "E006", "첨부파일 크기가 제한을 초과했습니다"),
      FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "E009", "파일을 찾을 수 없습니다"),

      // 이메일 내용 관련 오류
      EMPTY_EMAIL_CONTENT(HttpStatus.BAD_REQUEST, "E007", "이메일 내용이 비어있습니다"),
      INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "E008", "이메일 형식이 올바르지 않습니다"),

      // 입력값 검증 오류
      INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "E010", "잘못된 입력값입니다");

      private final HttpStatus httpStatus;
      private final String code;
      private final String message;
}
