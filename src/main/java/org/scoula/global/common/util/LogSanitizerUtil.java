package org.scoula.global.common.util;

/**
 * 로그 주입 공격을 방지하기 위한 유틸리티 클래스
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
public final class LogSanitizerUtil {

      private LogSanitizerUtil() {
          // 유틸리티 클래스이므로 인스턴스화 방지
      }

      /**
       * 로그에 안전하게 기록할 수 있도록 문자열을 sanitize합니다.
       *
       * @param input 입력 문자열
       * @return sanitize된 문자열 (never null)
       */
      public static String sanitize(String input) {
          if (input == null) {
              return "null";
          }

          // 개행 문자, 캐리지 리턴, 라인 구분자 등 모든 줄바꿈 관련 문자 제거
          String sanitized = input.replaceAll("[\r\n\u2028\u2029]", "_");

          // 로그 주입에 사용될 수 있는 특수 문자 이스케이프
          sanitized = sanitized.replaceAll("[\\p{Cntrl}&&[^\t]]", "_");

          // 길이 제한 (너무 긴 로그 방지)
          if (sanitized.length() > 1000) {
              sanitized = sanitized.substring(0, 997) + "...";
          }

          return sanitized;
      }

      /**
       * 객체를 문자열로 변환한 후 sanitize합니다. null-safe하며, Number 타입은 직접 toString()을 사용합니다.
       *
       * @param value 입력 값
       * @return sanitize된 문자열 (never null)
       */
      public static String sanitizeValue(Object value) {
          if (value == null) {
              return "null";
          }
          // Number 타입은 보안상 안전하므로 바로 toString
          if (value instanceof Number) {
              return value.toString();
          }
          // String인 경우 sanitize 메서드 호출
          if (value instanceof String) {
              return sanitize((String) value);
          }
          // 기타 객체는 toString 후 sanitize
          return sanitize(value.toString());
      }
}
