package org.scoula.global.common.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("NumberFormatUtil 테스트")
class NumberFormatUtilTest {

      private NumberFormatUtil numberFormatUtil;

      @BeforeEach
      void setUp() {
          numberFormatUtil = new NumberFormatUtil();
      }

      @Nested
      @DisplayName("formatWonShort 메서드 테스트")
      class FormatWonShortTest {

          @Test
          @DisplayName("0원 처리")
          void formatWonShort_Zero() {
              // Given
              int amount = 0;

              // When
              String result = numberFormatUtil.formatWonShort(amount);

              // Then
              assertEquals("0원", result);
          }

          @Test
          @DisplayName("만원 단위 - 기본")
          void formatWonShort_ManWon() {
              // Given
              int amount = 50_000;

              // When
              String result = numberFormatUtil.formatWonShort(amount);

              // Then
              assertEquals("5만원", result);
          }

          @Test
          @DisplayName("천만원 단위")
          void formatWonShort_CheonManWon() {
              // Given
              int amount = 15_000_000;

              // When
              String result = numberFormatUtil.formatWonShort(amount);

              // Then
              assertEquals("1천 500만원", result);
          }

          @Test
          @DisplayName("억원 단위 - 기본")
          void formatWonShort_EokWon() {
              // Given
              int amount = 200_000_000;

              // When
              String result = numberFormatUtil.formatWonShort(amount);

              // Then
              assertEquals("2억원", result);
          }

          @Test
          @DisplayName("억원 + 천만원")
          void formatWonShort_EokCheonManWon() {
              // Given
              int amount = 1_230_000_000;

              // When
              String result = numberFormatUtil.formatWonShort(amount);

              // Then
              assertEquals("12억 3천원", result);
          }

          @Test
          @DisplayName("억원 + 만원 (천만원 없음)")
          void formatWonShort_EokManWon() {
              // Given
              int amount = 1_050_000_000;

              // When
              String result = numberFormatUtil.formatWonShort(amount);

              // Then
              assertEquals("10억 5천원", result);
          }

          @ParameterizedTest
          @CsvSource({
              "10000, '1만원'",
              "100000, '10만원'",
              "1000000, '100만원'",
              "10000000, '1천원'",
              "50000000, '5천원'",
              "100000000, '1억원'"
          })
          @DisplayName("다양한 금액 패턴 테스트")
          void formatWonShort_VariousAmounts(int amount, String expected) {
              // When
              String result = numberFormatUtil.formatWonShort(amount);

              // Then
              assertEquals(expected, result);
          }
      }

      @Nested
      @DisplayName("toKoreanNumber 메서드 테스트")
      class ToKoreanNumberTest {

          @Test
          @DisplayName("0 처리")
          void toKoreanNumber_Zero() {
              // Given
              int amount = 0;

              // When
              String result = numberFormatUtil.toKoreanNumber(amount);

              // Then
              assertEquals("영", result);
          }

          @Test
          @DisplayName("한 자리 숫자")
          void toKoreanNumber_SingleDigit() {
              // Given
              int amount = 7;

              // When
              String result = numberFormatUtil.toKoreanNumber(amount);

              // Then
              assertEquals("칠", result);
          }

          @Test
          @DisplayName("십의 자리 - 일십은 십으로")
          void toKoreanNumber_Ten() {
              // Given
              int amount = 10;

              // When
              String result = numberFormatUtil.toKoreanNumber(amount);

              // Then
              assertEquals("십", result);
          }

          @Test
          @DisplayName("십의 자리 - 이십")
          void toKoreanNumber_Twenty() {
              // Given
              int amount = 20;

              // When
              String result = numberFormatUtil.toKoreanNumber(amount);

              // Then
              assertEquals("이십", result);
          }

          @Test
          @DisplayName("백의 자리 - 일백은 백으로")
          void toKoreanNumber_OneHundred() {
              // Given
              int amount = 100;

              // When
              String result = numberFormatUtil.toKoreanNumber(amount);

              // Then
              assertEquals("백", result);
          }

          @Test
          @DisplayName("천의 자리 - 일천은 천으로")
          void toKoreanNumber_OneThousand() {
              // Given
              int amount = 1000;

              // When
              String result = numberFormatUtil.toKoreanNumber(amount);

              // Then
              assertEquals("천", result);
          }

          @Test
          @DisplayName("만의 자리 - 일만")
          void toKoreanNumber_TenThousand() {
              // Given
              int amount = 10000;

              // When
              String result = numberFormatUtil.toKoreanNumber(amount);

              // Then
              assertEquals("일만", result);
          }

          @Test
          @DisplayName("억의 자리 - 일억")
          void toKoreanNumber_OneHundredMillion() {
              // Given
              int amount = 100000000;

              // When
              String result = numberFormatUtil.toKoreanNumber(amount);

              // Then
              assertEquals("일억", result);
          }

          @Test
          @DisplayName("복합 숫자 - 1234")
          void toKoreanNumber_Complex1234() {
              // Given
              int amount = 1234;

              // When
              String result = numberFormatUtil.toKoreanNumber(amount);

              // Then
              assertEquals("천이백삼십사", result);
          }

          @Test
          @DisplayName("복합 숫자 - 12345")
          void toKoreanNumber_Complex12345() {
              // Given
              int amount = 12345;

              // When
              String result = numberFormatUtil.toKoreanNumber(amount);

              // Then
              assertEquals("일만이천삼백사십오", result);
          }

          @ParameterizedTest
          @CsvSource({
              "1, '일'", "2, '이'", "3, '삼'", "4, '사'", "5, '오'", "6, '육'", "7, '칠'", "8, '팔'", "9, '구'"
          })
          @DisplayName("기본 숫자 변환 테스트")
          void toKoreanNumber_BasicDigits(int amount, String expected) {
              // When
              String result = numberFormatUtil.toKoreanNumber(amount);

              // Then
              assertEquals(expected, result);
          }

          @ParameterizedTest
          @CsvSource({
              "11, '십일'",
              "21, '이십일'",
              "101, '백일'",
              "111, '백십일'",
              "1001, '천일'",
              "1111, '천백십일'"
          })
          @DisplayName("복잡한 숫자 패턴 테스트")
          void toKoreanNumber_ComplexPatterns(int amount, String expected) {
              // When
              String result = numberFormatUtil.toKoreanNumber(amount);

              // Then
              assertEquals(expected, result);
          }

          @Test
          @DisplayName("큰 숫자 - 19091")
          void toKoreanNumber_LargeNumber() {
              // Given
              int amount = 19091;

              // When
              String result = numberFormatUtil.toKoreanNumber(amount);

              // Then
              assertEquals("일만구천구십일", result);
          }
      }

      @Nested
      @DisplayName("경계값 테스트")
      class BoundaryTest {

          @Test
          @DisplayName("최대값 근처 - formatWonShort")
          void formatWonShort_MaxValue() {
              // Given
              int amount = Integer.MAX_VALUE;

              // When & Then
              assertDoesNotThrow(() -> numberFormatUtil.formatWonShort(amount));
          }

          @Test
          @DisplayName("최대값 근처 - toKoreanNumber")
          void toKoreanNumber_MaxValue() {
              // Given
              int amount = Integer.MAX_VALUE;

              // When & Then
              assertDoesNotThrow(() -> numberFormatUtil.toKoreanNumber(amount));
          }

          @Test
          @DisplayName("음수 값 처리 - formatWonShort")
          void formatWonShort_NegativeValue() {
              // Given
              int amount = -1000;

              // When & Then
              assertDoesNotThrow(() -> numberFormatUtil.formatWonShort(amount));
          }

          @Test
          @DisplayName("음수 값 처리 - toKoreanNumber")
          void toKoreanNumber_NegativeValue() {
              // Given
              int amount = -1000;

              // When & Then
              assertDoesNotThrow(() -> numberFormatUtil.toKoreanNumber(amount));
          }
      }
}
