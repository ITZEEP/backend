package org.scoula.domain.home.exception;

/** 매물 등록 중 발생할 수 있는 예외 처리 클래스입니다. */
public class HomeRegisterException extends RuntimeException {

      public HomeRegisterException(String message) {
          super(message);
      }

      public HomeRegisterException(String message, Throwable cause) {
          super(message, cause);
      }
}
