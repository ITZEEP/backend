package org.scoula.domain.verification.service;

import org.scoula.domain.verification.dto.request.IdCardVerificationRequest;
import org.scoula.domain.verification.dto.response.IdCardVerificationResponse;
import org.scoula.domain.verification.exception.VerificationErrorCode;
import org.scoula.domain.verification.exception.VerificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * 주민등록증 진위 확인 서비스 구현체
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Log4j2
@Transactional(readOnly = true)
public class IdCardVerificationServiceImpl implements IdCardVerificationService {

      private final RestTemplate restTemplate;
      private final ObjectMapper objectMapper;

      @Value("${verification.api.url:https://apick.app/rest/identi_card/1}")
      private String apiUrl;

      @Value("${verification.api.key:}")
      private String apiKey;

      /** {@inheritDoc} */
      @Override
      public boolean verifyIdCard(IdCardVerificationRequest request) {
          log.info("주민등록증 진위 확인 시작: {}", request.getMaskedInfo());

          IdCardVerificationResponse response = getVerificationDetail(request);

          if (!response.isVerificationSuccessful()) {
              Integer result = response.getData().getResult();
              String msg = response.getData().getMsg();
              String errorMessage =
                      String.format(
                              "주민등록증 진위 확인 실패: %s (코드: %d)",
                              msg != null && !msg.isEmpty() ? msg : getResultMessage(result),
                              result != null ? result : -1);

              log.warn(errorMessage);

              // API는 구체적인 실패 이유를 제공하지 않으므로 일반적인 실패 예외를 던짐
              // result가 2인 경우 msg를 확인하여 더 구체적인 에러를 던질 수 있음
              if (result != null && result == 2 && msg != null && !msg.isEmpty()) {
                  // msg에 따라 구체적인 예외를 던질 수 있지만,
                  // 현재는 API가 어떤 msg를 반환하는지 명확하지 않음
                  throw new VerificationException(VerificationErrorCode.VERIFICATION_FAILED, msg);
              } else if (result != null && result == 3) {
                  throw new VerificationException(VerificationErrorCode.VERIFICATION_TIMEOUT);
              } else {
                  throw new VerificationException(
                          VerificationErrorCode.VERIFICATION_FAILED, errorMessage);
              }
          }

          log.info("주민등록증 진위 확인 성공");
          return true;
      }

      /** {@inheritDoc} */
      @Override
      public IdCardVerificationResponse getVerificationDetail(IdCardVerificationRequest request) {
          log.debug("주민등록증 진위 확인 상세 조회 시작");

          try {
              // 헤더 설정
              HttpHeaders headers = createHeaders();

              // FormData 설정
              MultiValueMap<String, String> formData = createFormData(request);

              // HTTP 요청 생성
              HttpEntity<MultiValueMap<String, String>> httpEntity =
                      new HttpEntity<>(formData, headers);

              // API 호출
              log.debug("API 호출: {}", apiUrl);
              ResponseEntity<String> responseEntity =
                      restTemplate.exchange(apiUrl, HttpMethod.POST, httpEntity, String.class);

              // 응답 처리
              if (responseEntity.getStatusCode() != HttpStatus.OK) {
                  log.error("API 응답 오류: HTTP {}", responseEntity.getStatusCode());
                  throw new VerificationException(
                          VerificationErrorCode.API_COMMUNICATION_ERROR,
                          "API 응답 상태 코드: " + responseEntity.getStatusCode());
              }

              // JSON 파싱
              String responseBody = responseEntity.getBody();
              log.debug("API 응답 수신 완료");

              IdCardVerificationResponse response = parseResponse(responseBody);

              // API 호출 성공 여부 확인
              if (!response.isApiSuccessful()) {
                  log.error("API 호출 실패: {}", response);
                  throw new VerificationException(
                          VerificationErrorCode.API_COMMUNICATION_ERROR, "API 호출이 실패했습니다");
              }

              // 결과 로깅
              String resultMessage = getResultMessage(response.getData().getResult());
              log.info("주민등록증 진위 확인 API 호출 완료 - 결과: {}", resultMessage);

              return response;

          } catch (VerificationException e) {
              // VerificationException은 그대로 전파
              throw e;
          } catch (HttpClientErrorException e) {
              log.error("API 클라이언트 오류: {}", e.getMessage());
              if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                  throw new VerificationException(VerificationErrorCode.INVALID_AUTH_KEY);
              }
              throw new VerificationException(
                      VerificationErrorCode.API_COMMUNICATION_ERROR,
                      "API 클라이언트 오류: " + e.getMessage(),
                      e);
          } catch (HttpServerErrorException e) {
              log.error("API 서버 오류: {}", e.getMessage());
              throw new VerificationException(
                      VerificationErrorCode.VERIFICATION_SERVICE_UNAVAILABLE,
                      "API 서버 오류: " + e.getMessage(),
                      e);
          } catch (ResourceAccessException e) {
              log.error("API 연결 오류: {}", e.getMessage());
              throw new VerificationException(
                      VerificationErrorCode.VERIFICATION_TIMEOUT, "API 연결 오류: " + e.getMessage(), e);
          } catch (Exception e) {
              log.error("예상치 못한 오류 발생", e);
              throw new VerificationException(
                      VerificationErrorCode.API_COMMUNICATION_ERROR,
                      "예상치 못한 오류: " + e.getMessage(),
                      e);
          }
      }

      /**
       * HTTP 헤더 생성
       *
       * @return HttpHeaders
       */
      private HttpHeaders createHeaders() {
          HttpHeaders headers = new HttpHeaders();
          headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

          // 인증키 설정
          String authKey = getAuthKey();
          headers.set("CL_AUTH_KEY", authKey);

          return headers;
      }

      /**
       * FormData 생성
       *
       * @param request 검증 요청 정보
       * @return MultiValueMap
       */
      private MultiValueMap<String, String> createFormData(IdCardVerificationRequest request) {
          MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
          formData.add("name", request.getName());
          formData.add("rrn1", request.getRrn1());
          formData.add("rrn2", request.getRrn2());
          formData.add("date", request.getDate());
          return formData;
      }

      /**
       * API 인증키 반환 설정된 API 인증키를 반환합니다.
       *
       * @return API 인증키
       */
      private String getAuthKey() {
          if (apiKey == null || apiKey.isEmpty()) {
              log.error("API 키가 설정되지 않았습니다");
              throw new VerificationException(
                      VerificationErrorCode.INVALID_AUTH_KEY, "API 키가 설정되지 않았습니다");
          }
          return apiKey;
      }

      /**
       * API 응답 파싱
       *
       * @param responseBody 응답 본문
       * @return IdCardVerificationResponse
       */
      private IdCardVerificationResponse parseResponse(String responseBody) {
          try {
              return objectMapper.readValue(responseBody, IdCardVerificationResponse.class);
          } catch (JsonProcessingException e) {
              log.error("API 응답 파싱 오류: {}", e.getMessage());
              log.debug("응답 본문: {}", responseBody);
              throw new VerificationException(
                      VerificationErrorCode.API_RESPONSE_PARSE_ERROR, "API 응답을 파싱할 수 없습니다", e);
          }
      }

      /**
       * 결과 코드에 대한 메시지 반환
       *
       * @param result 결과 코드
       * @return 결과 메시지
       */
      private String getResultMessage(Integer result) {
          if (result == null) {
              return "결과 없음";
          }

          switch (result) {
              case 0:
                  return "실패";
              case 1:
                  return "성공";
              case 2:
                  return "메시지 확인 필요";
              case 3:
                  return "실패(타임아웃)";
              default:
                  return "알 수 없는 결과(" + result + ")";
          }
      }
}
