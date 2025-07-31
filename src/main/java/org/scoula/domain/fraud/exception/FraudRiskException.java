package org.scoula.domain.fraud.exception;

import lombok.Getter;

@Getter
public class FraudRiskException extends RuntimeException {

      private final String errorCode;

      public FraudRiskException(String message) {
          super(message);
          this.errorCode = "FRAUD_RISK_ERROR";
      }

      public FraudRiskException(String message, String errorCode) {
          super(message);
          this.errorCode = errorCode;
      }

      public FraudRiskException(String message, Throwable cause) {
          super(message, cause);
          this.errorCode = "FRAUD_RISK_ERROR";
      }

      public FraudRiskException(String message, String errorCode, Throwable cause) {
          super(message, cause);
          this.errorCode = errorCode;
      }
}
