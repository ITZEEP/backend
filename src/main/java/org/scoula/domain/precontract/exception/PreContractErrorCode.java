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
      ENUM_VALUE_OF("TENANT_4002", HttpStatus.BAD_REQUEST, "올바른 이넘 타입이 아닙니다. 다시 확인해주세요");

      private final String code;
      private final HttpStatus httpStatus;
      private final String message;
}
