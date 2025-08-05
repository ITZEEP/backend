package org.scoula.domain.chat.service;

import org.scoula.domain.chat.dto.ai.ClauseImproveRequestDto;
import org.scoula.domain.chat.dto.ai.ClauseImproveResponseDto;
import org.scoula.domain.chat.exception.ChatErrorCode;
import org.scoula.global.common.exception.BusinessException;
import org.scoula.global.common.util.LogSanitizerUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class AiClauseImproveServiceImpl implements AiClauseImproveService {

      private final RestTemplate restTemplate;
      private final ObjectMapper objectMapper;

      @Value("${ai.server.url:http://localhost:8000}")
      private String aiServerUrl;

      @Override
      public ClauseImproveResponseDto improveClause(ClauseImproveRequestDto request) {
          try {
              String url = buildUrl("/api/clause/improve");
              logRequestStart(request);

              HttpEntity<ClauseImproveRequestDto> requestEntity = createRequestEntity(request);
              ResponseEntity<ClauseImproveResponseDto> response =
                      restTemplate.postForEntity(url, requestEntity, ClauseImproveResponseDto.class);

              return processResponse(response);

          } catch (BusinessException e) {
              throw e;
          } catch (Exception e) {
              log.error("AI 특약 개선 요청 실패", e);
              throw new BusinessException(ChatErrorCode.AI_SERVER_ERROR, e);
          }
      }

      private String buildUrl(String endpoint) {
          return aiServerUrl + endpoint;
      }

      private void logRequestStart(ClauseImproveRequestDto request) {
          log.info(
                  "AI 특약 개선 요청 시작 - contractChatId: {}, round: {}, order: {}",
                  request.getContractChatId(),
                  request.getRound(),
                  request.getOrder());
          logRequest(request);
      }

      private HttpEntity<ClauseImproveRequestDto> createRequestEntity(
              ClauseImproveRequestDto request) {
          HttpHeaders headers = new HttpHeaders();
          headers.setContentType(MediaType.APPLICATION_JSON);
          return new HttpEntity<>(request, headers);
      }

      private ClauseImproveResponseDto processResponse(
              ResponseEntity<ClauseImproveResponseDto> response) {
          if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
              log.error("AI 서버 응답 오류 - Status: {}", response.getStatusCode());
              throw new BusinessException(ChatErrorCode.AI_SERVER_ERROR);
          }

          ClauseImproveResponseDto responseBody = response.getBody();
          if (!responseBody.isSuccess() || responseBody.getData() == null) {
              log.error("AI 특약 개선 실패 - message: {}", LogSanitizerUtil.sanitize(responseBody.getMessage()));
              throw new BusinessException(
                      ChatErrorCode.AI_SERVER_ERROR,
                      responseBody.getMessage() != null ? responseBody.getMessage() : "특약 개선 실패");
          }

          logResponseSuccess(responseBody.getData());
          return responseBody;
      }

      private void logResponseSuccess(ClauseImproveResponseDto.Data data) {
          log.info(
                  "AI 특약 개선 완료 - round: {}, order: {}, title: {}",
                  data.getRound(),
                  data.getOrder(),
                  LogSanitizerUtil.sanitize(data.getTitle()));
      }

      private void logRequest(ClauseImproveRequestDto request) {
          try {
              log.debug("AI 특약 개선 요청 데이터: {}", objectMapper.writeValueAsString(request));
          } catch (Exception e) {
              log.error("요청 데이터 로깅 실패", e);
          }
      }
}
