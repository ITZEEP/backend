package org.scoula.global.common.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@DisplayName("HttpHeadersUtil 테스트")
class HttpHeadersUtilTest {

      @Nested
      @DisplayName("createFormUrlEncodedHeaders 메서드 테스트")
      class CreateFormUrlEncodedHeadersTest {

          @Test
          @DisplayName("Form URL Encoded 헤더 생성")
          void createFormUrlEncodedHeaders_Success() {
              // When
              HttpHeaders headers = HttpHeadersUtil.createFormUrlEncodedHeaders();

              // Then
              assertNotNull(headers);
              assertEquals(MediaType.APPLICATION_FORM_URLENCODED, headers.getContentType());
          }

          @Test
          @DisplayName("매번 새로운 HttpHeaders 인스턴스 반환")
          void createFormUrlEncodedHeaders_NewInstance() {
              // When
              HttpHeaders headers1 = HttpHeadersUtil.createFormUrlEncodedHeaders();
              HttpHeaders headers2 = HttpHeadersUtil.createFormUrlEncodedHeaders();

              // Then
              assertNotSame(headers1, headers2);
              assertEquals(headers1.getContentType(), headers2.getContentType());
          }
      }

      @Nested
      @DisplayName("createBearerAuthHeaders 메서드 테스트")
      class CreateBearerAuthHeadersTest {

          @Test
          @DisplayName("유효한 토큰으로 Bearer 인증 헤더 생성")
          void createBearerAuthHeaders_ValidToken() {
              // Given
              String token = "valid-jwt-token-12345";

              // When
              HttpHeaders headers = HttpHeadersUtil.createBearerAuthHeaders(token);

              // Then
              assertNotNull(headers);
              assertTrue(headers.containsKey(HttpHeaders.AUTHORIZATION));
              assertEquals("Bearer " + token, headers.getFirst(HttpHeaders.AUTHORIZATION));
          }

          @Test
          @DisplayName("빈 토큰으로 Bearer 인증 헤더 생성")
          void createBearerAuthHeaders_EmptyToken() {
              // Given
              String token = "";

              // When
              HttpHeaders headers = HttpHeadersUtil.createBearerAuthHeaders(token);

              // Then
              assertNotNull(headers);
              assertTrue(headers.containsKey(HttpHeaders.AUTHORIZATION));
              assertEquals("Bearer ", headers.getFirst(HttpHeaders.AUTHORIZATION));
          }

          @Test
          @DisplayName("null 토큰으로 Bearer 인증 헤더 생성")
          void createBearerAuthHeaders_NullToken() {
              // Given
              String token = null;

              // When
              HttpHeaders headers = HttpHeadersUtil.createBearerAuthHeaders(token);

              // Then
              assertNotNull(headers);
              assertTrue(headers.containsKey(HttpHeaders.AUTHORIZATION));
              assertEquals("Bearer null", headers.getFirst(HttpHeaders.AUTHORIZATION));
          }

          @Test
          @DisplayName("긴 토큰으로 Bearer 인증 헤더 생성")
          void createBearerAuthHeaders_LongToken() {
              // Given
              String token =
                      "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

              // When
              HttpHeaders headers = HttpHeadersUtil.createBearerAuthHeaders(token);

              // Then
              assertNotNull(headers);
              assertTrue(headers.containsKey(HttpHeaders.AUTHORIZATION));
              assertEquals("Bearer " + token, headers.getFirst(HttpHeaders.AUTHORIZATION));
          }
      }

      @Nested
      @DisplayName("createBearerAuthWithFormHeaders 메서드 테스트")
      class CreateBearerAuthWithFormHeadersTest {

          @Test
          @DisplayName("Bearer 인증과 Form URL Encoded 헤더 생성")
          void createBearerAuthWithFormHeaders_Success() {
              // Given
              String token = "test-token-123";

              // When
              HttpHeaders headers = HttpHeadersUtil.createBearerAuthWithFormHeaders(token);

              // Then
              assertNotNull(headers);
              assertTrue(headers.containsKey(HttpHeaders.AUTHORIZATION));
              assertEquals("Bearer " + token, headers.getFirst(HttpHeaders.AUTHORIZATION));
              assertEquals(MediaType.APPLICATION_FORM_URLENCODED, headers.getContentType());
          }

          @Test
          @DisplayName("두 헤더가 독립적으로 설정됨")
          void createBearerAuthWithFormHeaders_IndependentHeaders() {
              // Given
              String token = "independent-test-token";

              // When
              HttpHeaders headers = HttpHeadersUtil.createBearerAuthWithFormHeaders(token);

              // Then
              assertNotNull(headers);

              // Authorization 헤더 확인
              assertTrue(headers.containsKey(HttpHeaders.AUTHORIZATION));
              assertEquals("Bearer " + token, headers.getFirst(HttpHeaders.AUTHORIZATION));

              // Content-Type 헤더 확인
              assertEquals(MediaType.APPLICATION_FORM_URLENCODED, headers.getContentType());

              // 두 헤더가 모두 존재하는지 확인
              assertEquals(2, headers.size());
          }

          @Test
          @DisplayName("null 토큰과 Form 헤더 함께 생성")
          void createBearerAuthWithFormHeaders_NullToken() {
              // Given
              String token = null;

              // When
              HttpHeaders headers = HttpHeadersUtil.createBearerAuthWithFormHeaders(token);

              // Then
              assertNotNull(headers);
              assertTrue(headers.containsKey(HttpHeaders.AUTHORIZATION));
              assertEquals("Bearer null", headers.getFirst(HttpHeaders.AUTHORIZATION));
              assertEquals(MediaType.APPLICATION_FORM_URLENCODED, headers.getContentType());
          }
      }

      @Nested
      @DisplayName("createJsonHeaders 메서드 테스트")
      class CreateJsonHeadersTest {

          @Test
          @DisplayName("JSON Content-Type 헤더 생성")
          void createJsonHeaders_Success() {
              // When
              HttpHeaders headers = HttpHeadersUtil.createJsonHeaders();

              // Then
              assertNotNull(headers);
              assertEquals(MediaType.APPLICATION_JSON, headers.getContentType());
          }

          @Test
          @DisplayName("매번 새로운 HttpHeaders 인스턴스 반환")
          void createJsonHeaders_NewInstance() {
              // When
              HttpHeaders headers1 = HttpHeadersUtil.createJsonHeaders();
              HttpHeaders headers2 = HttpHeadersUtil.createJsonHeaders();

              // Then
              assertNotSame(headers1, headers2);
              assertEquals(headers1.getContentType(), headers2.getContentType());
          }

          @Test
          @DisplayName("Content-Type만 설정되고 다른 헤더는 없음")
          void createJsonHeaders_OnlyContentType() {
              // When
              HttpHeaders headers = HttpHeadersUtil.createJsonHeaders();

              // Then
              assertNotNull(headers);
              assertEquals(1, headers.size());
              assertEquals(MediaType.APPLICATION_JSON, headers.getContentType());
          }
      }

      @Nested
      @DisplayName("메서드 간 독립성 테스트")
      class MethodIndependenceTest {

          @Test
          @DisplayName("다른 메서드들이 서로 영향을 주지 않음")
          void methods_AreIndependent() {
              // Given
              String token = "independence-test-token";

              // When
              HttpHeaders formHeaders = HttpHeadersUtil.createFormUrlEncodedHeaders();
              HttpHeaders bearerHeaders = HttpHeadersUtil.createBearerAuthHeaders(token);
              HttpHeaders combinedHeaders = HttpHeadersUtil.createBearerAuthWithFormHeaders(token);
              HttpHeaders jsonHeaders = HttpHeadersUtil.createJsonHeaders();

              // Then
              // Form 헤더는 Content-Type만 가짐
              assertEquals(1, formHeaders.size());
              assertEquals(MediaType.APPLICATION_FORM_URLENCODED, formHeaders.getContentType());
              assertFalse(formHeaders.containsKey(HttpHeaders.AUTHORIZATION));

              // Bearer 헤더는 Authorization만 가짐
              assertEquals(1, bearerHeaders.size());
              assertTrue(bearerHeaders.containsKey(HttpHeaders.AUTHORIZATION));
              assertNull(bearerHeaders.getContentType());

              // Combined 헤더는 둘 다 가짐
              assertEquals(2, combinedHeaders.size());
              assertEquals(MediaType.APPLICATION_FORM_URLENCODED, combinedHeaders.getContentType());
              assertTrue(combinedHeaders.containsKey(HttpHeaders.AUTHORIZATION));

              // JSON 헤더는 Content-Type만 가짐
              assertEquals(1, jsonHeaders.size());
              assertEquals(MediaType.APPLICATION_JSON, jsonHeaders.getContentType());
              assertFalse(jsonHeaders.containsKey(HttpHeaders.AUTHORIZATION));
          }

          @Test
          @DisplayName("모든 메서드가 새로운 인스턴스 반환")
          void allMethods_ReturnNewInstances() {
              // Given
              String token = "new-instance-test";

              // When
              HttpHeaders headers1 = HttpHeadersUtil.createFormUrlEncodedHeaders();
              HttpHeaders headers2 = HttpHeadersUtil.createBearerAuthHeaders(token);
              HttpHeaders headers3 = HttpHeadersUtil.createBearerAuthWithFormHeaders(token);
              HttpHeaders headers4 = HttpHeadersUtil.createJsonHeaders();

              // Then
              assertNotSame(headers1, headers2);
              assertNotSame(headers1, headers3);
              assertNotSame(headers1, headers4);
              assertNotSame(headers2, headers3);
              assertNotSame(headers2, headers4);
              assertNotSame(headers3, headers4);
          }
      }
}
