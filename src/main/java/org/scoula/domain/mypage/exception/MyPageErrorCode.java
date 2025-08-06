package org.scoula.domain.mypage.exception;

import org.scoula.global.common.exception.IErrorCode;
import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MyPageErrorCode implements IErrorCode {

      // 사용자 정보 관련
      USER_NOT_FOUND(HttpStatus.NOT_FOUND, "MP001", "사용자를 찾을 수 없습니다."),

      // 닉네임 관련
      DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "MP002", "이미 사용 중인 닉네임입니다."),
      INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "MP003", "유효하지 않은 닉네임입니다."),

      // 프로필 이미지 관련
      INVALID_IMAGE_FORMAT(HttpStatus.BAD_REQUEST, "MP004", "지원하지 않는 이미지 형식입니다."),
      IMAGE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "MP005", "이미지 크기가 제한을 초과했습니다."),
      IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "MP006", "이미지 업로드에 실패했습니다."),
      EMPTY_IMAGE_FILE(HttpStatus.BAD_REQUEST, "MP011", "빈 이미지 파일입니다."),
      INVALID_IMAGE_NAME(HttpStatus.BAD_REQUEST, "MP012", "유효하지 않은 이미지 파일명입니다."),

      // 데이터 조회 관련
      CONTRACT_NOT_FOUND(HttpStatus.NOT_FOUND, "MP007", "계약서를 찾을 수 없습니다."),
      PROPERTY_NOT_FOUND(HttpStatus.NOT_FOUND, "MP008", "매물을 찾을 수 없습니다."),
      RISK_ANALYSIS_NOT_FOUND(HttpStatus.NOT_FOUND, "MP009", "사기위험도 분석 결과를 찾을 수 없습니다."),

      // 권한 관련
      UNAUTHORIZED_ACCESS(HttpStatus.FORBIDDEN, "MP010", "접근 권한이 없습니다.");

      private final HttpStatus httpStatus;
      private final String code;
      private final String message;
}
