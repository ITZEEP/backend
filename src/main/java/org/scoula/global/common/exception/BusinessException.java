package org.scoula.global.common.exception;

/** 비즈니스 로직 예외 클래스 업무 규칙 위반이나 비즈니스 로직 오류 시 사용 */
public class BusinessException extends BaseException {

      public BusinessException(IErrorCode errorCode) {
          super(errorCode);
      }

      public BusinessException(IErrorCode errorCode, String message) {
          super(errorCode, message);
      }

      public BusinessException(IErrorCode errorCode, Throwable cause) {
          super(errorCode, cause);
      }

      public BusinessException(IErrorCode errorCode, String message, Throwable cause) {
          super(errorCode, message, cause);
      }
}
