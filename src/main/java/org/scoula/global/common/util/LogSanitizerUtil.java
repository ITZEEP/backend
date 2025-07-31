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
       * 객체를 로그에 안전하게 기록할 수 있도록 문자열로 변환합니다.
       *
       * @param obj 입력 객체
       * @return sanitize된 문자열
       */
      public static String sanitize(Object obj) {
          if (obj == null) {
              return "null";
          }
          // Number 타입은 특별 처리 (보안상 안전하므로 바로 toString)
          if (obj instanceof Number) {
              return obj.toString();
          }
          return sanitize(obj.toString());
      }
}
