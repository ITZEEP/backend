package org.scoula.domain.fraud.exception;

import org.scoula.global.common.exception.BusinessException;
import org.scoula.global.common.exception.IErrorCode;

public class FraudRiskException extends BusinessException {

      public FraudRiskException(IErrorCode errorCode) {
          super(errorCode);
      }

      public FraudRiskException(IErrorCode errorCode, String message) {
          super(errorCode, message);
      }

      public FraudRiskException(IErrorCode errorCode, Throwable cause) {
          super(errorCode, cause);
      }

      public FraudRiskException(IErrorCode errorCode, String message, Throwable cause) {
          super(errorCode, message, cause);
      }

      /**
       * 테스트 전용 생성자 - 프로덕션 코드에서는 사용하지 않을 것을 권장
       *
       * @deprecated 테스트 목적으로만 사용. 프로덕션에서는 IErrorCode를 사용하는 생성자 사용
       */
      @Deprecated
      public FraudRiskException(String message) {
          super(FraudErrorCode.FRAUD_ANALYSIS_FAILED, message);
      }
}
