package org.scoula.global.common.exception;

/** 입력값이 유효하지 않을 때 발생하는 예외 */
public class InvalidValueException extends BaseException {

      public InvalidValueException(String message) {
          super(CommonErrorCode.INVALID_INPUT_VALUE, message);
      }

      public InvalidValueException() {
          super(CommonErrorCode.INVALID_INPUT_VALUE);
      }

      public InvalidValueException(String value, String fieldName) {
          super(
                  CommonErrorCode.INVALID_INPUT_VALUE,
                  String.format("필드 '%s'의 값 '%s'이(가) 유효하지 않습니다", fieldName, value));
      }
}
