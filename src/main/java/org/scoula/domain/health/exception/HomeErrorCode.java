package org.scoula.domain.health.exception;

import org.scoula.global.common.exception.IErrorCode;
import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HomeErrorCode implements IErrorCode {
      SERVICE_UNAVAILABLE("HOME_001", HttpStatus.SERVICE_UNAVAILABLE, "서비스가 일시적으로 사용 불가능합니다."),
      RATE_LIMIT_EXCEEDED("HOME_002", HttpStatus.TOO_MANY_REQUESTS, "요청 횟수를 초과했습니다. 잠시 후 다시 시도해주세요."),
      INVALID_REQUEST("HOME_003", HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
      INTERNAL_ERROR("HOME_004", HttpStatus.INTERNAL_SERVER_ERROR, "내부 서버 오류가 발생했습니다.");

      private final String code;
      private final HttpStatus httpStatus;
      private final String message;
}
