package org.scoula.global.common.util;

import java.util.Collection;
import java.util.regex.Pattern;

import org.scoula.global.common.exception.BusinessException;
import org.scoula.global.common.exception.IErrorCode;
import org.springframework.util.StringUtils;

/** 공통 검증 유틸리티 클래스 여러 서비스에서 사용되는 검증 로직을 중앙화 */
public class ValidationUtils {

      // 이메일 정규식 패턴
      private static final Pattern EMAIL_PATTERN =
              Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

      // 파일명 정규식 패턴 (안전한 파일명)
      private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");

      private ValidationUtils() {
          // 유틸리티 클래스는 인스턴스화 방지
      }

      /** 문자열이 비어있지 않은지 검증 */
      public static void validateNotEmpty(String value, String fieldName, IErrorCode errorCode) {
          if (!StringUtils.hasText(value)) {
              throw new BusinessException(errorCode, fieldName + " is required");
          }
      }

      /** 문자열 길이 검증 */
      public static void validateLength(
              String value, String fieldName, int minLength, int maxLength, IErrorCode errorCode) {
          if (value == null) {
              throw new BusinessException(errorCode, fieldName + " is required");
          }

          int length = value.length();
          if (length < minLength || length > maxLength) {
              throw new BusinessException(
                      errorCode,
                      String.format(
                              "%s length must be between %d and %d characters",
                              fieldName, minLength, maxLength));
          }
      }

      /** 숫자 범위 검증 */
      public static void validateRange(
              long value, String fieldName, long min, long max, IErrorCode errorCode) {
          if (value < min || value > max) {
              throw new BusinessException(
                      errorCode, String.format("%s must be between %d and %d", fieldName, min, max));
          }
      }

      /** 컬렉션이 비어있지 않은지 검증 */
      public static void validateNotEmpty(
              Collection<?> collection, String fieldName, IErrorCode errorCode) {
          if (collection == null || collection.isEmpty()) {
              throw new BusinessException(errorCode, fieldName + " cannot be empty");
          }
      }

      /** 이메일 형식 검증 */
      public static void validateEmail(String email, IErrorCode errorCode) {
          validateNotEmpty(email, "Email", errorCode);

          if (!EMAIL_PATTERN.matcher(email).matches()) {
              throw new BusinessException(errorCode, "올바른 이메일 형식이 아닙니다");
          }
      }

      /** 파일명 안전성 검증 */
      public static void validateSafeFilename(String filename, IErrorCode errorCode) {
          validateNotEmpty(filename, "Filename", errorCode);

          if (!SAFE_FILENAME_PATTERN.matcher(filename).matches()) {
              throw new BusinessException(errorCode, "Invalid filename format");
          }
      }

      /** 객체가 null이 아닌지 검증 */
      public static void validateNotNull(Object object, String fieldName, IErrorCode errorCode) {
          if (object == null) {
              throw new BusinessException(errorCode, fieldName + " cannot be null");
          }
      }

      /** 조건이 참인지 검증 */
      public static void validateCondition(boolean condition, String message, IErrorCode errorCode) {
          if (!condition) {
              throw new BusinessException(errorCode, message);
          }
      }

      /** 인증 상태 검증 */
      public static void validateAuthentication(
              org.springframework.security.core.Authentication authentication) {
          if (authentication == null || !authentication.isAuthenticated()) {
              throw new BusinessException(
                      org.scoula.global.common.exception.CommonErrorCode.INVALID_INPUT_VALUE,
                      "Authentication is required");
          }
      }

      /** 문자열이 특정 패턴과 일치하는지 검증 */
      public static void validatePattern(
              String value, Pattern pattern, String fieldName, IErrorCode errorCode) {
          validateNotEmpty(value, fieldName, errorCode);

          if (!pattern.matcher(value).matches()) {
              throw new BusinessException(errorCode, fieldName + " has invalid format");
          }
      }
}
