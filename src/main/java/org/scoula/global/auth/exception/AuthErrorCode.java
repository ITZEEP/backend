package org.scoula.global.auth.exception;

import org.scoula.global.common.exception.IErrorCode;
import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 인증/인가 관련 에러 코드 */
@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements IErrorCode {

      // 인증 관련 오류
      AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "A001", "인증에 실패했습니다"),
      UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "A002", "인증되지 않은 접근입니다"),
      INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "유효하지 않은 토큰입니다"),
      EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A004", "만료된 토큰입니다"),
      INSUFFICIENT_PRIVILEGES(HttpStatus.FORBIDDEN, "A005", "권한이 부족합니다"),
      TOKEN_BLACKLISTED(HttpStatus.UNAUTHORIZED, "A006", "차단된 토큰입니다"),

      // 사용자 관련 오류
      USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다"),
      EMAIL_DUPLICATION(HttpStatus.CONFLICT, "U002", "이미 사용 중인 이메일입니다"),
      USERNAME_DUPLICATION(HttpStatus.CONFLICT, "U003", "이미 사용 중인 사용자명입니다"),
      INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "U004", "비밀번호가 올바르지 않습니다"),
      INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "U005", "이메일 형식이 올바르지 않습니다");

      private final HttpStatus httpStatus;
      private final String code;
      private final String message;
}
