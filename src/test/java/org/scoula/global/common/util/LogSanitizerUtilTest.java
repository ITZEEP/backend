package org.scoula.global.common.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("LogSanitizerUtil 테스트")
class LogSanitizerUtilTest {

      @Nested
      @DisplayName("sanitize 메서드 테스트")
      class SanitizeTest {

          @Test
          @DisplayName("null 입력 처리")
          void sanitize_NullInput() {
              // Given & When
              String result = LogSanitizerUtil.sanitize(null);

              // Then
              assertEquals("null", result);
          }

          @Test
          @DisplayName("빈 문자열 처리")
          void sanitize_EmptyString() {
              // Given
              String input = "";

              // When
              String result = LogSanitizerUtil.sanitize(input);

              // Then
              assertEquals("", result);
          }

          @Test
          @DisplayName("정상 문자열 처리")
          void sanitize_NormalString() {
              // Given
              String input = "안전한 로그 메시지입니다";

              // When
              String result = LogSanitizerUtil.sanitize(input);

              // Then
              assertEquals("안전한 로그 메시지입니다", result);
          }

          @Test
          @DisplayName("개행 문자 제거 - LF")
          void sanitize_LineFeed() {
              // Given
              String input = "첫 번째 줄\n두 번째 줄";

              // When
              String result = LogSanitizerUtil.sanitize(input);

              // Then
              assertEquals("첫 번째 줄_두 번째 줄", result);
          }

          @Test
          @DisplayName("개행 문자 제거 - CR")
          void sanitize_CarriageReturn() {
              // Given
              String input = "첫 번째 줄\r두 번째 줄";

              // When
              String result = LogSanitizerUtil.sanitize(input);

              // Then
              assertEquals("첫 번째 줄_두 번째 줄", result);
          }

          @Test
          @DisplayName("개행 문자 제거 - CRLF")
          void sanitize_CarriageReturnLineFeed() {
              // Given
              String input = "첫 번째 줄\r\n두 번째 줄";

              // When
              String result = LogSanitizerUtil.sanitize(input);

              // Then
              assertEquals("첫 번째 줄__두 번째 줄", result);
          }

          @Test
          @DisplayName("유니코드 라인 구분자 제거")
          void sanitize_UnicodeLineSeparators() {
              // Given
              String input = "첫 번째 줄\u2028두 번째 줄\u2029세 번째 줄";

              // When
              String result = LogSanitizerUtil.sanitize(input);

              // Then
              assertEquals("첫 번째 줄_두 번째 줄_세 번째 줄", result);
          }

          @Test
          @DisplayName("제어 문자 제거")
          void sanitize_ControlCharacters() {
              // Given
              String input = "정상\u0001제어\u0002문자\u0007포함";

              // When
              String result = LogSanitizerUtil.sanitize(input);

              // Then
              assertEquals("정상_제어_문자_포함", result);
          }

          @Test
          @DisplayName("탭 문자는 유지")
          void sanitize_PreserveTab() {
              // Given
              String input = "탭\t문자는\t유지";

              // When
              String result = LogSanitizerUtil.sanitize(input);

              // Then
              assertEquals("탭\t문자는\t유지", result);
          }

          @Test
          @DisplayName("길이 제한 - 1000자 이하")
          void sanitize_LengthUnder1000() {
              // Given
              String input = "a".repeat(999);

              // When
              String result = LogSanitizerUtil.sanitize(input);

              // Then
              assertEquals(999, result.length());
              assertEquals(input, result);
          }

          @Test
          @DisplayName("길이 제한 - 정확히 1000자")
          void sanitize_LengthExactly1000() {
              // Given
              String input = "a".repeat(1000);

              // When
              String result = LogSanitizerUtil.sanitize(input);

              // Then
              assertEquals(1000, result.length());
              assertEquals(input, result);
          }

          @Test
          @DisplayName("길이 제한 - 1000자 초과")
          void sanitize_LengthOver1000() {
              // Given
              String input = "a".repeat(1001);

              // When
              String result = LogSanitizerUtil.sanitize(input);

              // Then
              assertEquals(1000, result.length());
              assertTrue(result.endsWith("..."));
              assertEquals("a".repeat(997) + "...", result);
          }

          @Test
          @DisplayName("다양한 공격 패턴 sanitize - 로그 주입")
          void sanitize_LogInjection() {
              // Given
              String input = "로그 주입 시도\nfake log entry";

              // When
              String result = LogSanitizerUtil.sanitize(input);

              // Then
              assertEquals("로그 주입 시도_fake log entry", result);
          }

          @Test
          @DisplayName("다양한 공격 패턴 sanitize - SQL 인젝션")
          void sanitize_SqlInjection() {
              // Given
              String input = "SQL Injection\r\nDROP TABLE";

              // When
              String result = LogSanitizerUtil.sanitize(input);

              // Then
              assertEquals("SQL Injection__DROP TABLE", result);
          }

          @Test
          @DisplayName("다양한 공격 패턴 sanitize - XSS")
          void sanitize_XssAttack() {
              // Given
              String input = "XSS Attack\u2028<script>";

              // When
              String result = LogSanitizerUtil.sanitize(input);

              // Then
              assertEquals("XSS Attack_<script>", result);
          }

          @Test
          @DisplayName("다양한 공격 패턴 sanitize - 제어 문자")
          void sanitize_ControlChars() {
              // Given
              String input = "정상\u0000무효화\u0001시도";

              // When
              String result = LogSanitizerUtil.sanitize(input);

              // Then
              assertEquals("정상_무효화_시도", result);
          }
      }

      @Nested
      @DisplayName("sanitizeValue 메서드 테스트")
      class SanitizeValueTest {

          @Test
          @DisplayName("null 값 처리")
          void sanitizeValue_NullValue() {
              // Given & When
              String result = LogSanitizerUtil.sanitizeValue(null);

              // Then
              assertEquals("null", result);
          }

          @Test
          @DisplayName("Integer 숫자 처리")
          void sanitizeValue_Integer() {
              // Given
              Integer value = 12345;

              // When
              String result = LogSanitizerUtil.sanitizeValue(value);

              // Then
              assertEquals("12345", result);
          }

          @Test
          @DisplayName("Long 숫자 처리")
          void sanitizeValue_Long() {
              // Given
              Long value = 9876543210L;

              // When
              String result = LogSanitizerUtil.sanitizeValue(value);

              // Then
              assertEquals("9876543210", result);
          }

          @Test
          @DisplayName("Double 숫자 처리")
          void sanitizeValue_Double() {
              // Given
              Double value = 123.456;

              // When
              String result = LogSanitizerUtil.sanitizeValue(value);

              // Then
              assertEquals("123.456", result);
          }

          @Test
          @DisplayName("String 값 처리")
          void sanitizeValue_String() {
              // Given
              String value = "안전한\n문자열\r테스트";

              // When
              String result = LogSanitizerUtil.sanitizeValue(value);

              // Then
              assertEquals("안전한_문자열_테스트", result);
          }

          @Test
          @DisplayName("객체 toString 후 sanitize")
          void sanitizeValue_Object() {
              // Given
              Object value = new TestObject("테스트\n객체");

              // When
              String result = LogSanitizerUtil.sanitizeValue(value);

              // Then
              assertEquals("TestObject{name='테스트_객체'}", result);
          }

          @ParameterizedTest
          @ValueSource(ints = {0, -1, 100, Integer.MAX_VALUE, Integer.MIN_VALUE})
          @DisplayName("다양한 정수값 처리")
          void sanitizeValue_VariousIntegers(int value) {
              // When
              String result = LogSanitizerUtil.sanitizeValue(value);

              // Then
              assertEquals(String.valueOf(value), result);
          }

          @Test
          @DisplayName("Boolean 값 처리")
          void sanitizeValue_Boolean() {
              // When & Then
              assertEquals("true", LogSanitizerUtil.sanitizeValue(true));
              assertEquals("false", LogSanitizerUtil.sanitizeValue(false));
          }
      }

      /** 테스트용 내부 클래스 */
      private static class TestObject {
          private final String name;

          public TestObject(String name) {
              this.name = name;
          }

          @Override
          public String toString() {
              return "TestObject{name='" + name + "'}";
          }
      }
}
