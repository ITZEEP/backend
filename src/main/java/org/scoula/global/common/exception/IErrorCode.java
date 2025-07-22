package org.scoula.global.common.exception;

import org.springframework.http.HttpStatus;

/** 에러 코드 인터페이스 각 도메인별로 구현하여 사용 */
public interface IErrorCode {

      /**
       * HTTP 상태 코드를 반환합니다.
       *
       * @return HTTP 상태 코드
       */
      HttpStatus getHttpStatus();

      /**
       * 에러 코드를 반환합니다.
       *
       * @return 에러 코드 문자열
       */
      String getCode();

      /**
       * 에러 메시지를 반환합니다.
       *
       * @return 에러 메시지
       */
      String getMessage();
}
