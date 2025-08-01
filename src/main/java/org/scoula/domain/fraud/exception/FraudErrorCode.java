package org.scoula.domain.fraud.exception;

import org.scoula.global.common.exception.IErrorCode;
import org.springframework.http.HttpStatus;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum FraudErrorCode implements IErrorCode {
      // 기본 에러
      INVALID_INPUT_VALUE("FRAUD_001", HttpStatus.BAD_REQUEST, "잘못된 입력값입니다"),

      // 사기 위험도 분석 관련 에러
      FRAUD_ANALYSIS_FAILED("FRAUD_002", HttpStatus.INTERNAL_SERVER_ERROR, "사기 위험도 분석에 실패했습니다"),
      RISK_CALCULATION_ERROR("FRAUD_003", HttpStatus.INTERNAL_SERVER_ERROR, "위험도 계산 중 오류가 발생했습니다"),
      INVALID_DOCUMENT_FORMAT("FRAUD_004", HttpStatus.BAD_REQUEST, "유효하지 않은 문서 형식입니다"),
      DOCUMENT_PROCESSING_FAILED("FRAUD_005", HttpStatus.INTERNAL_SERVER_ERROR, "문서 처리에 실패했습니다"),

      // 사기 검증 관련 에러
      FRAUD_CHECK_NOT_FOUND("FRAUD_006", HttpStatus.NOT_FOUND, "위험도 체크 결과를 찾을 수 없습니다"),
      FRAUD_CHECK_ALREADY_EXISTS("FRAUD_007", HttpStatus.CONFLICT, "이미 진행 중인 사기 검증이 있습니다"),
      FRAUD_CHECK_ACCESS_DENIED("FRAUD_008", HttpStatus.FORBIDDEN, "해당 위험도 체크 결과에 대한 권한이 없습니다"),

      // OCR 및 문서 분석 관련 에러
      OCR_PROCESSING_FAILED("FRAUD_009", HttpStatus.INTERNAL_SERVER_ERROR, "OCR 처리에 실패했습니다"),
      DOCUMENT_NOT_FOUND("FRAUD_010", HttpStatus.NOT_FOUND, "문서를 찾을 수 없습니다"),
      UNSUPPORTED_DOCUMENT_TYPE("FRAUD_011", HttpStatus.BAD_REQUEST, "지원하지 않는 문서 형식입니다"),

      // AI 분석 관련 에러
      AI_SERVICE_UNAVAILABLE("FRAUD_012", HttpStatus.SERVICE_UNAVAILABLE, "AI 분석 서비스를 사용할 수 없습니다"),
      AI_ANALYSIS_TIMEOUT("FRAUD_013", HttpStatus.REQUEST_TIMEOUT, "AI 분석 처리 시간이 초과되었습니다"),

      // 데이터 검증 관련 에러
      INVALID_CONTRACT_DATA("FRAUD_014", HttpStatus.BAD_REQUEST, "유효하지 않은 계약 데이터입니다"),
      MISSING_REQUIRED_FIELDS("FRAUD_015", HttpStatus.BAD_REQUEST, "필수 정보가 누락되었습니다"),
      DATA_CONSISTENCY_ERROR("FRAUD_016", HttpStatus.CONFLICT, "데이터 일관성 오류가 발생했습니다");

      private final String code;
      private final HttpStatus httpStatus;
      private final String message;

      @Override
      public String getCode() {
          return code;
      }

      @Override
      public HttpStatus getHttpStatus() {
          return httpStatus;
      }

      @Override
      public String getMessage() {
          return message;
      }
}
