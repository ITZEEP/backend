package org.scoula.domain.verification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scoula.domain.verification.dto.request.IdCardVerificationRequest;
import org.scoula.domain.verification.dto.response.IdCardVerificationResponse;
import org.scoula.domain.verification.exception.VerificationErrorCode;
import org.scoula.domain.verification.exception.VerificationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * IdCardVerificationServiceImpl 테스트
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IdCardVerificationServiceImpl 테스트")
class IdCardVerificationServiceImplTest {

      @Mock private RestTemplate restTemplate;

      @Mock private ObjectMapper objectMapper;

      @InjectMocks private IdCardVerificationServiceImpl verificationService;

      private IdCardVerificationRequest request;
      private String apiUrl = "https://apick.app/rest/identi_card/1";
      private String apiKey = "test-api-key";

      @BeforeEach
      void setUp() {
          // 테스트 요청 데이터 생성
          request =
                  IdCardVerificationRequest.builder()
                          .name("홍길동")
                          .rrn1("901231")
                          .rrn2("1234567")
                          .date("20230115")
                          .build();

          // @Value 필드 주입
          ReflectionTestUtils.setField(verificationService, "apiUrl", apiUrl);
          ReflectionTestUtils.setField(verificationService, "apiKey", apiKey);
          ReflectionTestUtils.setField(verificationService, "timeout", 30000);
      }

      @Test
      @DisplayName("verifyIdCard - 검증 성공 시 true를 반환한다")
      void verifyIdCard_Success() throws JsonProcessingException {
          // given
          String responseJson = createSuccessResponseJson();
          IdCardVerificationResponse mockResponse = createSuccessResponse();

          when(restTemplate.exchange(
                          eq(apiUrl), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                  .thenReturn(new ResponseEntity<>(responseJson, HttpStatus.OK));

          when(objectMapper.readValue(responseJson, IdCardVerificationResponse.class))
                  .thenReturn(mockResponse);

          // when
          boolean result = verificationService.verifyIdCard(request);

          // then
          assertThat(result).isTrue();
          verify(restTemplate)
                  .exchange(eq(apiUrl), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
      }

      @Test
      @DisplayName("verifyIdCard - 검증 실패 시 VerificationException을 발생시킨다")
      void verifyIdCard_Failure() throws JsonProcessingException {
          // given
          String responseJson = createFailureResponseJson();
          IdCardVerificationResponse mockResponse = createFailureResponse();

          when(restTemplate.exchange(
                          eq(apiUrl), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                  .thenReturn(new ResponseEntity<>(responseJson, HttpStatus.OK));

          when(objectMapper.readValue(responseJson, IdCardVerificationResponse.class))
                  .thenReturn(mockResponse);

          // when & then
          assertThatThrownBy(() -> verificationService.verifyIdCard(request))
                  .isInstanceOf(VerificationException.class)
                  .satisfies(
                          thrown -> {
                              VerificationException ex = (VerificationException) thrown;
                              assertThat(ex.getErrorCode())
                                      .isEqualTo(VerificationErrorCode.VERIFICATION_FAILED);
                          });
      }

      @Test
      @DisplayName("verifyIdCard - 타임아웃 시 VERIFICATION_TIMEOUT 예외를 발생시킨다")
      void verifyIdCard_Timeout() throws JsonProcessingException {
          // given
          String responseJson = createTimeoutResponseJson();
          IdCardVerificationResponse mockResponse = createTimeoutResponse();

          when(restTemplate.exchange(
                          eq(apiUrl), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                  .thenReturn(new ResponseEntity<>(responseJson, HttpStatus.OK));

          when(objectMapper.readValue(responseJson, IdCardVerificationResponse.class))
                  .thenReturn(mockResponse);

          // when & then
          assertThatThrownBy(() -> verificationService.verifyIdCard(request))
                  .isInstanceOf(VerificationException.class)
                  .satisfies(
                          thrown -> {
                              VerificationException ex = (VerificationException) thrown;
                              assertThat(ex.getErrorCode())
                                      .isEqualTo(VerificationErrorCode.VERIFICATION_TIMEOUT);
                          });
      }

      @Test
      @DisplayName("getVerificationDetail - API 호출 성공 시 응답을 반환한다")
      void getVerificationDetail_Success() throws JsonProcessingException {
          // given
          String responseJson = createSuccessResponseJson();
          IdCardVerificationResponse expectedResponse = createSuccessResponse();

          when(restTemplate.exchange(
                          eq(apiUrl), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                  .thenReturn(new ResponseEntity<>(responseJson, HttpStatus.OK));

          when(objectMapper.readValue(responseJson, IdCardVerificationResponse.class))
                  .thenReturn(expectedResponse);

          // when
          IdCardVerificationResponse result = verificationService.getVerificationDetail(request);

          // then
          assertThat(result).isNotNull();
          assertThat(result.isVerificationSuccessful()).isTrue();
          assertThat(result.isApiSuccessful()).isTrue();
      }

      @Test
      @DisplayName("getVerificationDetail - API 키가 설정되지 않은 경우 INVALID_AUTH_KEY 예외를 발생시킨다")
      void getVerificationDetail_NoApiKey() {
          // given
          ReflectionTestUtils.setField(verificationService, "apiKey", null);

          // when & then
          assertThatThrownBy(() -> verificationService.getVerificationDetail(request))
                  .isInstanceOf(VerificationException.class)
                  .satisfies(
                          thrown -> {
                              VerificationException ex = (VerificationException) thrown;
                              assertThat(ex.getErrorCode())
                                      .isEqualTo(VerificationErrorCode.INVALID_AUTH_KEY);
                          });
      }

      // Helper methods for creating test data
      private String createSuccessResponseJson() {
          return "{"
                  + "\"data\": {"
                  + "\"ic_id\": 99721,"
                  + "\"type\": 1,"
                  + "\"result\": 1,"
                  + "\"msg\": \"\","
                  + "\"success\": 1"
                  + "},"
                  + "\"api\": {"
                  + "\"success\": true,"
                  + "\"cost\": 40,"
                  + "\"ms\": 4032,"
                  + "\"pl_id\": 103894"
                  + "}"
                  + "}";
      }

      private String createFailureResponseJson() {
          return "{"
                  + "\"data\": {"
                  + "\"ic_id\": 99721,"
                  + "\"type\": 1,"
                  + "\"result\": 0,"
                  + "\"msg\": \"주민등록증 진위 확인 실패\","
                  + "\"success\": 0"
                  + "},"
                  + "\"api\": {"
                  + "\"success\": true,"
                  + "\"cost\": 40,"
                  + "\"ms\": 4032,"
                  + "\"pl_id\": 103894"
                  + "}"
                  + "}";
      }

      private String createTimeoutResponseJson() {
          return "{"
                  + "\"data\": {"
                  + "\"ic_id\": 99721,"
                  + "\"type\": 1,"
                  + "\"result\": 3,"
                  + "\"msg\": \"타임아웃\","
                  + "\"success\": 3"
                  + "},"
                  + "\"api\": {"
                  + "\"success\": true,"
                  + "\"cost\": 40,"
                  + "\"ms\": 4032,"
                  + "\"pl_id\": 103894"
                  + "}"
                  + "}";
      }

      private IdCardVerificationResponse createSuccessResponse() {
          return IdCardVerificationResponse.builder()
                  .data(
                          IdCardVerificationResponse.VerificationData.builder()
                                  .icId(99721)
                                  .type(1)
                                  .result(1)
                                  .msg("")
                                  .success(1)
                                  .build())
                  .api(
                          IdCardVerificationResponse.ApiInfo.builder()
                                  .success(true)
                                  .cost(40)
                                  .ms(4032L)
                                  .plId(103894L)
                                  .build())
                  .build();
      }

      private IdCardVerificationResponse createFailureResponse() {
          return IdCardVerificationResponse.builder()
                  .data(
                          IdCardVerificationResponse.VerificationData.builder()
                                  .icId(99721)
                                  .type(1)
                                  .result(0)
                                  .msg("주민등록증 진위 확인 실패")
                                  .success(0)
                                  .build())
                  .api(
                          IdCardVerificationResponse.ApiInfo.builder()
                                  .success(true)
                                  .cost(40)
                                  .ms(4032L)
                                  .plId(103894L)
                                  .build())
                  .build();
      }

      private IdCardVerificationResponse createTimeoutResponse() {
          return IdCardVerificationResponse.builder()
                  .data(
                          IdCardVerificationResponse.VerificationData.builder()
                                  .icId(99721)
                                  .type(1)
                                  .result(3)
                                  .msg("타임아웃")
                                  .success(3)
                                  .build())
                  .api(
                          IdCardVerificationResponse.ApiInfo.builder()
                                  .success(true)
                                  .cost(40)
                                  .ms(4032L)
                                  .plId(103894L)
                                  .build())
                  .build();
      }
}
