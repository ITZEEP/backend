package org.scoula.global.common.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * HTTP 헤더 유틸리티 클래스
 *
 * <p>공통적으로 사용되는 HTTP 헤더 생성 메서드를 제공합니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
public class HttpHeadersUtil {

      private HttpHeadersUtil() {
          // 유틸리티 클래스 인스턴스화 방지
      }

      /**
       * Form URL Encoded 헤더 생성
       *
       * @return Form URL Encoded Content-Type이 설정된 HttpHeaders
       */
      public static HttpHeaders createFormUrlEncodedHeaders() {
          HttpHeaders headers = new HttpHeaders();
          headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
          return headers;
      }

      /**
       * Bearer 인증 헤더 생성
       *
       * @param token Bearer 토큰
       * @return Bearer 인증이 설정된 HttpHeaders
       */
      public static HttpHeaders createBearerAuthHeaders(String token) {
          HttpHeaders headers = new HttpHeaders();
          headers.setBearerAuth(token);
          return headers;
      }

      /**
       * Bearer 인증과 Form URL Encoded가 모두 설정된 헤더 생성
       *
       * @param token Bearer 토큰
       * @return Bearer 인증과 Form URL Encoded가 설정된 HttpHeaders
       */
      public static HttpHeaders createBearerAuthWithFormHeaders(String token) {
          HttpHeaders headers = new HttpHeaders();
          headers.setBearerAuth(token);
          headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
          return headers;
      }

      /**
       * JSON Content-Type 헤더 생성
       *
       * @return JSON Content-Type이 설정된 HttpHeaders
       */
      public static HttpHeaders createJsonHeaders() {
          HttpHeaders headers = new HttpHeaders();
          headers.setContentType(MediaType.APPLICATION_JSON);
          return headers;
      }
}
