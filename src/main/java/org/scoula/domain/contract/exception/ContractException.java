package org.scoula.domain.contract.exception;

import org.scoula.global.common.exception.IErrorCode;
import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContractException implements IErrorCode {
      CONTRACT_GET("CONTRACT_4001", HttpStatus.BAD_REQUEST, "mongoDB에서 값을 조회하지 못 했습니다."),
      CONTRACT_INSERT("CONTRACT_4002", HttpStatus.BAD_REQUEST, "MongoDB에 저장이 되지 않았습니다."),
      CONTRACT_AI_SERVER_ERROR(
              "CONTRACT_4003", HttpStatus.SERVICE_UNAVAILABLE, "AI 서버 통신 중 오류가 발생했습니다."),
      CONTRACT_UPDATE("CONTRACT_4004", HttpStatus.BAD_REQUEST, "MongoDB에 수정이 되지 않았습니다"),
      CONTRACT_REDIS("CONTRACT_4005", HttpStatus.BAD_REQUEST, "REDIS에 해당 정보가 없습니다.");
      private final String code;
      private final HttpStatus httpStatus;
      private final String message;
}
