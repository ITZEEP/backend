package org.scoula.domain.precontract.exception;

import org.scoula.global.common.exception.IErrorCode;
import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PreContractErrorCode implements IErrorCode {
      TENANT_RESPONSE(
              "TENANT_4001", HttpStatus.BAD_REQUEST, "DB에서 올바른 값을 가져오지 못해 response 값을 만들지 못했습니다."),
      ENUM_VALUE_OF("TENANT_4002", HttpStatus.BAD_REQUEST, "올바른 이넘 타입이 아닙니다. 다시 확인해주세요"),
      TENANT_UPDATE("TENANT_4003", HttpStatus.BAD_REQUEST, "값이 저장되지 않았습니다. (update fail)"),
      TENANT_SELECT("TENANT_4004", HttpStatus.BAD_REQUEST, "조회 결과가 없습니다"),
      TENANT_INSERT("TENANT_4005", HttpStatus.BAD_REQUEST, "데이터베이스 저장 실패 (insert fail)"),
      TENANT_USER("TENANT_4006", HttpStatus.BAD_REQUEST, "데이터베이스 유저와 현재 유저가 다릅니다"),
      TENANT_NULL("TENANT_4007", HttpStatus.BAD_REQUEST, "입력되지 않았습니다.");

      private final String code;
      private final HttpStatus httpStatus;
      private final String message;
}
