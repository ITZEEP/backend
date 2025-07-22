package org.scoula.global.common.exception;

import lombok.Getter;

/** 기본 예외 클래스 모든 커스텀 예외의 부모 클래스 */
@Getter
public abstract class BaseException extends RuntimeException {

      private final IErrorCode errorCode;

      protected BaseException(IErrorCode errorCode) {
          super(errorCode.getMessage());
          this.errorCode = errorCode;
      }

      protected BaseException(IErrorCode errorCode, String message) {
          super(message);
          this.errorCode = errorCode;
      }

      protected BaseException(IErrorCode errorCode, Throwable cause) {
          super(errorCode.getMessage(), cause);
          this.errorCode = errorCode;
      }

      protected BaseException(IErrorCode errorCode, String message, Throwable cause) {
          super(message, cause);
          this.errorCode = errorCode;
      }
}
