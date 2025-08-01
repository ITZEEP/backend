package org.scoula.domain.verification.exception;

import org.scoula.global.common.exception.BusinessException;

/**
 * 본인인증 도메인 예외
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
public class VerificationException extends BusinessException {

      /**
       * 에러 코드로 예외 생성
       *
       * @param errorCode 에러 코드
       */
      public VerificationException(VerificationErrorCode errorCode) {
          super(errorCode);
      }

      /**
       * 에러 코드와 추가 메시지로 예외 생성
       *
       * @param errorCode 에러 코드
       * @param message 추가 메시지
       */
      public VerificationException(VerificationErrorCode errorCode, String message) {
          super(errorCode, message);
      }

      /**
       * 에러 코드와 원인 예외로 예외 생성
       *
       * @param errorCode 에러 코드
       * @param cause 원인 예외
       */
      public VerificationException(VerificationErrorCode errorCode, Throwable cause) {
          super(errorCode, cause);
      }

      /**
       * 에러 코드, 추가 메시지, 원인 예외로 예외 생성
       *
       * @param errorCode 에러 코드
       * @param message 추가 메시지
       * @param cause 원인 예외
       */
      public VerificationException(VerificationErrorCode errorCode, String message, Throwable cause) {
          super(errorCode, message, cause);
      }
}
