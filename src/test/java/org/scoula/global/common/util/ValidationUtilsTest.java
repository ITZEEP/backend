package org.scoula.global.common.util;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.scoula.global.common.exception.BusinessException;
import org.scoula.global.common.exception.CommonErrorCode;

@DisplayName("검증 유틸리티 단위 테스트")
class ValidationUtilsTest {

      @Test
      @DisplayName("문자열 비어있지 않음 검증 - 유효한 값")
      void validateNotEmpty_ShouldPassForValidString() {
          // given
          String validValue = "test";

          // when & then
          assertThatNoException()
                  .isThrownBy(
                          () ->
                                  ValidationUtils.validateNotEmpty(
                                          validValue,
                                          "testField",
                                          CommonErrorCode.INVALID_INPUT_VALUE));
      }

      @Test
      @DisplayName("문자열 비어있지 않음 검증 - 빈 값으로 예외 발생")
      void validateNotEmpty_ShouldThrowExceptionForEmptyString() {
          // given
          String emptyValue = "";

          // when & then
          assertThatThrownBy(
                          () ->
                                  ValidationUtils.validateNotEmpty(
                                          emptyValue,
                                          "testField",
                                          CommonErrorCode.INVALID_INPUT_VALUE))
                  .isInstanceOf(BusinessException.class)
                  .hasMessageContaining("testField is required");
      }

      @Test
      @DisplayName("문자열 길이 검증 - 유효한 길이")
      void validateLength_ShouldPassForValidLength() {
          // given
          String validValue = "test";

          // when & then
          assertThatNoException()
                  .isThrownBy(
                          () ->
                                  ValidationUtils.validateLength(
                                          validValue,
                                          "testField",
                                          1,
                                          10,
                                          CommonErrorCode.INVALID_INPUT_VALUE));
      }

      @Test
      @DisplayName("문자열 길이 검증 - 너무 긴 값으로 예외 발생")
      void validateLength_ShouldThrowExceptionForTooLongString() {
          // given
          String tooLongValue = "this is a very long string";

          // when & then
          assertThatThrownBy(
                          () ->
                                  ValidationUtils.validateLength(
                                          tooLongValue,
                                          "testField",
                                          1,
                                          5,
                                          CommonErrorCode.INVALID_INPUT_VALUE))
                  .isInstanceOf(BusinessException.class)
                  .hasMessageContaining("testField length must be between 1 and 5 characters");
      }

      @Test
      @DisplayName("숫자 범위 검증 - 유효한 범위")
      void validateRange_ShouldPassForValidRange() {
          // given
          long validValue = 5;

          // when & then
          assertThatNoException()
                  .isThrownBy(
                          () ->
                                  ValidationUtils.validateRange(
                                          validValue,
                                          "testField",
                                          1,
                                          10,
                                          CommonErrorCode.INVALID_INPUT_VALUE));
      }

      @Test
      @DisplayName("숫자 범위 검증 - 범위 초과로 예외 발생")
      void validateRange_ShouldThrowExceptionForOutOfRange() {
          // given
          long outOfRangeValue = 15;

          // when & then
          assertThatThrownBy(
                          () ->
                                  ValidationUtils.validateRange(
                                          outOfRangeValue,
                                          "testField",
                                          1,
                                          10,
                                          CommonErrorCode.INVALID_INPUT_VALUE))
                  .isInstanceOf(BusinessException.class)
                  .hasMessageContaining("testField must be between 1 and 10");
      }

      @Test
      @DisplayName("컬렉션 비어있지 않음 검증 - 유효한 컬렉션")
      void validateNotEmpty_ShouldPassForNonEmptyCollection() {
          // given
          var validCollection = Arrays.asList("item1", "item2");

          // when & then
          assertThatNoException()
                  .isThrownBy(
                          () ->
                                  ValidationUtils.validateNotEmpty(
                                          validCollection,
                                          "testField",
                                          CommonErrorCode.INVALID_INPUT_VALUE));
      }

      @Test
      @DisplayName("컬렉션 비어있지 않음 검증 - 빈 컬렉션으로 예외 발생")
      void validateNotEmpty_ShouldThrowExceptionForEmptyCollection() {
          // given
          var emptyCollection = Collections.emptyList();

          // when & then
          assertThatThrownBy(
                          () ->
                                  ValidationUtils.validateNotEmpty(
                                          emptyCollection,
                                          "testField",
                                          CommonErrorCode.INVALID_INPUT_VALUE))
                  .isInstanceOf(BusinessException.class)
                  .hasMessageContaining("testField cannot be empty");
      }

      @Test
      @DisplayName("이메일 검증 - 유효한 이메일")
      void validateEmail_ShouldPassForValidEmail() {
          // given
          String validEmail = "test@example.com";

          // when & then
          assertThatNoException()
                  .isThrownBy(
                          () ->
                                  ValidationUtils.validateEmail(
                                          validEmail, CommonErrorCode.INVALID_INPUT_VALUE));
      }

      @Test
      @DisplayName("이메일 검증 - 잘못된 형식으로 예외 발생")
      void validateEmail_ShouldThrowExceptionForInvalidEmail() {
          // given
          String invalidEmail = "invalid-email";

          // when & then
          assertThatThrownBy(
                          () ->
                                  ValidationUtils.validateEmail(
                                          invalidEmail, CommonErrorCode.INVALID_INPUT_VALUE))
                  .isInstanceOf(BusinessException.class)
                  .hasMessageContaining("올바른 이메일 형식이 아닙니다");
      }

      @Test
      @DisplayName("파일명 안전성 검증 - 유효한 파일명")
      void validateSafeFilename_ShouldPassForSafeFilename() {
          // given
          String safeFilename = "test-file_name.txt";

          // when & then
          assertThatNoException()
                  .isThrownBy(
                          () ->
                                  ValidationUtils.validateSafeFilename(
                                          safeFilename, CommonErrorCode.INVALID_INPUT_VALUE));
      }

      @Test
      @DisplayName("파일명 안전성 검증 - 위험한 파일명으로 예외 발생")
      void validateSafeFilename_ShouldThrowExceptionForUnsafeFilename() {
          // given
          String unsafeFilename = "../../../etc/passwd";

          // when & then
          assertThatThrownBy(
                          () ->
                                  ValidationUtils.validateSafeFilename(
                                          unsafeFilename, CommonErrorCode.INVALID_INPUT_VALUE))
                  .isInstanceOf(BusinessException.class)
                  .hasMessageContaining("Invalid filename format");
      }

      @Test
      @DisplayName("객체 null 검증 - 유효한 객체")
      void validateNotNull_ShouldPassForNonNullObject() {
          // given
          String validObject = "test";

          // when & then
          assertThatNoException()
                  .isThrownBy(
                          () ->
                                  ValidationUtils.validateNotNull(
                                          validObject,
                                          "testField",
                                          CommonErrorCode.INVALID_INPUT_VALUE));
      }

      @Test
      @DisplayName("객체 null 검증 - null 객체로 예외 발생")
      void validateNotNull_ShouldThrowExceptionForNullObject() {
          // given
          String nullObject = null;

          // when & then
          assertThatThrownBy(
                          () ->
                                  ValidationUtils.validateNotNull(
                                          nullObject,
                                          "testField",
                                          CommonErrorCode.INVALID_INPUT_VALUE))
                  .isInstanceOf(BusinessException.class)
                  .hasMessageContaining("testField cannot be null");
      }

      @Test
      @DisplayName("조건 검증 - 참 조건")
      void validateCondition_ShouldPassForTrueCondition() {
          // when & then
          assertThatNoException()
                  .isThrownBy(
                          () ->
                                  ValidationUtils.validateCondition(
                                          true, "test message", CommonErrorCode.INVALID_INPUT_VALUE));
      }

      @Test
      @DisplayName("조건 검증 - 거짓 조건으로 예외 발생")
      void validateCondition_ShouldThrowExceptionForFalseCondition() {
          // when & then
          assertThatThrownBy(
                          () ->
                                  ValidationUtils.validateCondition(
                                          false, "test message", CommonErrorCode.INVALID_INPUT_VALUE))
                  .isInstanceOf(BusinessException.class)
                  .hasMessageContaining("test message");
      }

      @Test
      @DisplayName("패턴 검증 - 유효한 패턴")
      void validatePattern_ShouldPassForMatchingPattern() {
          // given
          String validValue = "123";
          Pattern numberPattern = Pattern.compile("\\d+");

          // when & then
          assertThatNoException()
                  .isThrownBy(
                          () ->
                                  ValidationUtils.validatePattern(
                                          validValue,
                                          numberPattern,
                                          "testField",
                                          CommonErrorCode.INVALID_INPUT_VALUE));
      }

      @Test
      @DisplayName("패턴 검증 - 패턴 불일치로 예외 발생")
      void validatePattern_ShouldThrowExceptionForNonMatchingPattern() {
          // given
          String invalidValue = "abc";
          Pattern numberPattern = Pattern.compile("\\d+");

          // when & then
          assertThatThrownBy(
                          () ->
                                  ValidationUtils.validatePattern(
                                          invalidValue,
                                          numberPattern,
                                          "testField",
                                          CommonErrorCode.INVALID_INPUT_VALUE))
                  .isInstanceOf(BusinessException.class)
                  .hasMessageContaining("testField has invalid format");
      }
}
