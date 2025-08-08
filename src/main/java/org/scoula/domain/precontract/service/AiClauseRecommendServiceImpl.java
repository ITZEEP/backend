package org.scoula.domain.precontract.service;

import org.scoula.domain.precontract.dto.ai.ClauseRecommendRequestDto;
import org.scoula.domain.precontract.dto.ai.ClauseRecommendResponseDto;
import org.scoula.domain.precontract.exception.OwnerPreContractErrorCode;
import org.scoula.global.common.exception.BusinessException;
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
public class AiClauseRecommendServiceImpl implements AiClauseRecommendService {

      private final RestTemplate restTemplate;
      private final ObjectMapper objectMapper;

      @Value("${ai.server.url:http://localhost:8000}")
      private String aiServerUrl;

      @Override
      public ClauseRecommendResponseDto recommendClauses(ClauseRecommendRequestDto request) {
          try {
              String url = aiServerUrl + "/api/clause/recommend";
              log.info("AI 특약 추천 요청 시작 - URL: {}", url);

              HttpHeaders headers = new HttpHeaders();
              headers.setContentType(MediaType.APPLICATION_JSON);

              HttpEntity<ClauseRecommendRequestDto> requestEntity =
                      new HttpEntity<>(request, headers);
              ResponseEntity<ClauseRecommendResponseDto> response =
                      restTemplate.postForEntity(
                              url, requestEntity, ClauseRecommendResponseDto.class);

              if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                  log.info(
                          "AI 특약 추천 완료 - 추천 특약 수: {}",
                          response.getBody().getData() != null
                                  ? response.getBody().getData().getTotalClauses()
                                  : 0);
                  return response.getBody();
              } else {
                  log.error(response.getBody());
                  log.error(requestEntity.getBody());
                  log.error("AI 서버 응답 오류 - Status: {}", response.getStatusCode());
                  throw new BusinessException(OwnerPreContractErrorCode.AI_SERVER_ERROR);
              }

          } catch (Exception e) {
              log.error(request.toString());
              log.error("AI 특약 추천 요청 실패", e);
              throw new BusinessException(OwnerPreContractErrorCode.AI_SERVER_ERROR, e);
          }
      }
}
