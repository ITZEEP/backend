package org.scoula.domain.precontract.exception;

import org.scoula.global.common.exception.IErrorCode;
import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OwnerPreContractErrorCode implements IErrorCode {
      // 계약 흐름에서 예외
      OWNER_RESPONSE("OWNER_001", HttpStatus.BAD_REQUEST, "임대인 응답 처리 중 오류가 발생했습니다."),

      // Enum 처리 오류
      ENUM_VALUE_OF("OWNER_002", HttpStatus.BAD_REQUEST, "유효하지 않은 ENUM 값입니다."),

      // UPDATE 실패
      OWNER_UPDATE("OWNER_003", HttpStatus.INTERNAL_SERVER_ERROR, "임대인 정보 업데이트에 실패했습니다."),

      // SELECT 실패
      OWNER_SELECT("OWNER_004", HttpStatus.NOT_FOUND, "임대인 정보를 찾을 수 없습니다."),

      // INSERT 실패
      OWNER_INSERT("OWNER_005", HttpStatus.INTERNAL_SERVER_ERROR, "임대인 정보 저장에 실패했습니다."),

      // 사용자 검증 실패
      OWNER_USER("OWNER_006", HttpStatus.FORBIDDEN, "임대인 사용자 정보가 일치하지 않습니다.");

      private final String code;
      private final HttpStatus httpStatus;
      private final String message;
}
