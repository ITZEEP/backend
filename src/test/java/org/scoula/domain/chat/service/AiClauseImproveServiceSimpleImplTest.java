package org.scoula.domain.chat.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scoula.domain.chat.dto.ai.ClauseImproveRequestDto;
import org.scoula.domain.chat.dto.ai.ClauseImproveResponseDto;
import org.scoula.global.common.exception.BusinessException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiClauseImproveServiceImpl 간단 테스트")
class AiClauseImproveServiceSimpleImplTest {

      @Mock private RestTemplate restTemplate;
      @Mock private ObjectMapper objectMapper;

      @InjectMocks private AiClauseImproveServiceImpl aiClauseImproveService;

      @BeforeEach
      void setUp() {
          ReflectionTestUtils.setField(
                  aiClauseImproveService, "aiServerUrl", "http://localhost:8000");
      }

      @Test
      @DisplayName("특약 개선 요청 null 체크")
      void improveClause_NullRequest() {
          // When & Then
          assertThrows(BusinessException.class, () -> aiClauseImproveService.improveClause(null));
      }

      @Test
      @DisplayName("AI 서버 통신 실패시 예외 발생")
      void improveClause_ServerError() {
          // Given
          ClauseImproveRequestDto request = new ClauseImproveRequestDto();

          when(restTemplate.postForEntity(
                          anyString(), any(HttpEntity.class), eq(ClauseImproveResponseDto.class)))
                  .thenThrow(new RestClientException("서버 연결 실패"));

          // When & Then
          assertThrows(BusinessException.class, () -> aiClauseImproveService.improveClause(request));
          verify(restTemplate)
                  .postForEntity(
                          anyString(), any(HttpEntity.class), eq(ClauseImproveResponseDto.class));
      }

      @Test
      @DisplayName("AI 서버 4xx 에러 응답 처리")
      void improveClause_BadRequest() {
          // Given
          ClauseImproveRequestDto request = new ClauseImproveRequestDto();

          when(restTemplate.postForEntity(
                          anyString(), any(HttpEntity.class), eq(ClauseImproveResponseDto.class)))
                  .thenReturn(new ResponseEntity<>(HttpStatus.BAD_REQUEST));

          // When & Then
          assertThrows(BusinessException.class, () -> aiClauseImproveService.improveClause(request));
      }

      @Test
      @DisplayName("AI 서버 5xx 에러 응답 처리")
      void improveClause_ServerInternalError() {
          // Given
          ClauseImproveRequestDto request = new ClauseImproveRequestDto();

          when(restTemplate.postForEntity(
                          anyString(), any(HttpEntity.class), eq(ClauseImproveResponseDto.class)))
                  .thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

          // When & Then
          assertThrows(BusinessException.class, () -> aiClauseImproveService.improveClause(request));
      }

      @Test
      @DisplayName("AI 서버 정상 응답하지만 body가 null인 경우")
      void improveClause_NullResponseBody() {
          // Given
          ClauseImproveRequestDto request = new ClauseImproveRequestDto();

          ResponseEntity<ClauseImproveResponseDto> responseEntity =
                  new ResponseEntity<>(null, HttpStatus.OK);

          when(restTemplate.postForEntity(
                          anyString(), any(HttpEntity.class), eq(ClauseImproveResponseDto.class)))
                  .thenReturn(responseEntity);

          // When & Then
          assertThrows(BusinessException.class, () -> aiClauseImproveService.improveClause(request));
      }
}
