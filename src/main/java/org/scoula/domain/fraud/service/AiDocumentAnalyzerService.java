package org.scoula.domain.fraud.service;

import java.io.IOException;

import org.scoula.domain.fraud.dto.ai.AiParseResponse;
import org.scoula.domain.fraud.exception.FraudErrorCode;
import org.scoula.domain.fraud.exception.FraudRiskException;
import org.scoula.global.common.util.LogSanitizerUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class AiDocumentAnalyzerService {

      private final RestTemplate restTemplate;

      @Value("${ai.server.url:http://localhost:8000}")
      private String aiServerUrl;

      /** 등기부등본 파일을 AI 서버로 전송하여 파싱 */
      public AiParseResponse analyzeRegistryDocument(MultipartFile file) throws IOException {
          String url = aiServerUrl + "/api/parse/register";
          return sendFileToAiServer(file, url);
      }

      /** 건축물대장 파일을 AI 서버로 전송하여 파싱 */
      public AiParseResponse analyzeBuildingDocument(MultipartFile file) throws IOException {
          String url = aiServerUrl + "/api/parse/building";
          return sendFileToAiServer(file, url);
      }

      /** 파일을 AI 서버로 전송하는 공통 메소드 */
      private AiParseResponse sendFileToAiServer(MultipartFile file, String url) throws IOException {
          log.info(
                  "AI 서버로 파일 전송 시작 - URL: {}, 파일명: {}",
                  LogSanitizerUtil.sanitize(url),
                  LogSanitizerUtil.sanitize(file.getOriginalFilename()));

          // HTTP 헤더 설정
          HttpHeaders headers = new HttpHeaders();
          headers.setContentType(MediaType.MULTIPART_FORM_DATA);

          // 멀티파트 폼 데이터 생성
          MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
          body.add(
                  "file",
                  new ByteArrayResource(file.getBytes()) {
                      @Override
                      public String getFilename() {
                          return file.getOriginalFilename();
                      }
                  });

          // HTTP 요청 엔티티 생성
          HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

          try {
              // AI 서버로 POST 요청 전송
              ResponseEntity<AiParseResponse> response =
                      restTemplate.exchange(
                              url, HttpMethod.POST, requestEntity, AiParseResponse.class);

              log.info(
                      "AI 서버 응답 성공 - 상태: {}",
                      LogSanitizerUtil.sanitizeValue(response.getStatusCode()));

              // AI 서버 응답 검증
              AiParseResponse aiResponse = response.getBody();
              if (aiResponse == null) {
                  throw new FraudRiskException(
                          FraudErrorCode.AI_SERVICE_UNAVAILABLE, "AI 서버로부터 빈 응답을 받았습니다");
              }

              if (!aiResponse.isSuccess()) {
                  throw new FraudRiskException(
                          FraudErrorCode.DOCUMENT_PROCESSING_FAILED,
                          "AI 파싱 실패: " + aiResponse.getMessage());
              }

              return aiResponse;

          } catch (HttpClientErrorException e) {
              // 4xx 클라이언트 오류
              log.error(
                      "AI 서버 클라이언트 오류 - URL: {}, 상태: {}, 메시지: {}",
                      LogSanitizerUtil.sanitize(url),
                      LogSanitizerUtil.sanitizeValue(e.getStatusCode()),
                      LogSanitizerUtil.sanitize(e.getMessage()));
              throw new FraudRiskException(
                      FraudErrorCode.INVALID_DOCUMENT_FORMAT, "AI 서버 요청 오류: " + e.getMessage(), e);
          } catch (HttpServerErrorException e) {
              // 5xx 서버 오류
              log.error(
                      "AI 서버 내부 오류 - URL: {}, 상태: {}, 메시지: {}",
                      LogSanitizerUtil.sanitize(url),
                      LogSanitizerUtil.sanitizeValue(e.getStatusCode()),
                      LogSanitizerUtil.sanitize(e.getMessage()));
              throw new FraudRiskException(
                      FraudErrorCode.AI_SERVICE_UNAVAILABLE, "AI 서버 내부 오류가 발생했습니다", e);
          } catch (ResourceAccessException e) {
              // 네트워크 연결 오류
              log.error(
                      "AI 서버 연결 실패 - URL: {}, 오류: {}",
                      LogSanitizerUtil.sanitize(url),
                      LogSanitizerUtil.sanitize(e.getMessage()));
              throw new FraudRiskException(
                      FraudErrorCode.AI_SERVICE_UNAVAILABLE, "AI 서버에 연결할 수 없습니다", e);
          } catch (FraudRiskException e) {
              // FraudRiskException은 그대로 다시 던지기
              throw e;
          } catch (Exception e) {
              // 기타 예상치 못한 오류
              log.error(
                      "AI 서버 통신 중 예상치 못한 오류 - URL: {}, 오류: {}",
                      LogSanitizerUtil.sanitize(url),
                      LogSanitizerUtil.sanitize(e.getMessage()),
                      e);
              throw new FraudRiskException(
                      FraudErrorCode.AI_SERVICE_UNAVAILABLE,
                      "AI 서버 통신 중 오류가 발생했습니다: " + e.getMessage(),
                      e);
          }
      }
}
