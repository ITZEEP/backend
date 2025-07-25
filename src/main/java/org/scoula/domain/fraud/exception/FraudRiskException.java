package org.scoula.domain.fraud.exception;

public class FraudRiskException extends RuntimeException {

      public FraudRiskException(String message) {
          super(message);
      }

      public FraudRiskException(String message, Throwable cause) {
          super(message, cause);
      }
}
