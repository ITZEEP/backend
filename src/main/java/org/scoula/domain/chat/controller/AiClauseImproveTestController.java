package org.scoula.domain.chat.controller;

import java.util.ArrayList;
import java.util.List;

import org.scoula.domain.chat.dto.ContentDataDto;
import org.scoula.domain.chat.dto.ai.ClauseImproveRequestDto;
import org.scoula.domain.chat.dto.ai.ClauseImproveResponseDto;
import org.scoula.domain.chat.service.AiClauseImproveService;
import org.scoula.domain.precontract.service.PreContractDataService;
import org.scoula.global.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Api(tags = "AI 특약 개선 테스트")
@RestController
@RequestMapping("/api/chat/ai-clause-test")
@RequiredArgsConstructor
@Log4j2
public class AiClauseImproveTestController {

      private final AiClauseImproveService aiClauseImproveService;
      private final PreContractDataService preContractDataService;

      @ApiOperation("AI 특약 개선 테스트")
      @PostMapping("/improve/{contractChatId}")
      public ResponseEntity<ApiResponse<ClauseImproveResponseDto>> testImproveClause(
              @PathVariable Long contractChatId, @RequestBody TestImproveRequestDto testRequest) {

          log.info("AI 특약 개선 테스트 시작 - contractChatId: {}", contractChatId);

          try {
              // 1. Owner 데이터 조회
              ClauseImproveRequestDto.OwnerData ownerData =
                      preContractDataService.fetchOwnerData(contractChatId);

              // 2. Tenant 데이터 조회
              ClauseImproveRequestDto.TenantData tenantData =
                      preContractDataService.fetchTenantData(contractChatId);

              // 3. OCR 데이터 조회
              ClauseImproveRequestDto.OcrData ocrData =
                      preContractDataService.fetchOcrData(contractChatId);

              // 4. 이전 특약 데이터 설정 (테스트용)
              List<ContentDataDto> prevClauses = convertPrevClauses(testRequest.getPrevClauses());

              // 5. 최근 특약 데이터 설정 (테스트용)
              ContentDataDto recentClause = convertRecentClause(testRequest.getRecentClause());

              // 6. AI 특약 개선 요청
              ClauseImproveRequestDto aiRequest =
                      ClauseImproveRequestDto.builder()
                              .contractChatId(contractChatId)
                              .ocrData(ocrData)
                              .round(testRequest.getRound())
                              .order(testRequest.getOrder())
                              .ownerData(ownerData)
                              .tenantData(tenantData)
                              .prevData(prevClauses)
                              .recentData(recentClause)
                              .build();

              ClauseImproveResponseDto response = aiClauseImproveService.improveClause(aiRequest);

              return ResponseEntity.ok(ApiResponse.success(response));

          } catch (Exception e) {
              log.error("AI 특약 개선 테스트 실패", e);
              return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
          }
      }

      // 테스트 요청 DTO
      public static class TestImproveRequestDto {
          private Long round;
          private Long order;
          private List<ContentDataDto> prevClauses;
          private ContentDataDto recentClause;

          // Getters and Setters
          public Long getRound() {
              return round;
          }

          public void setRound(Long round) {
              this.round = round;
          }

          public Long getOrder() {
              return order;
          }

          public void setOrder(Long order) {
              this.order = order;
          }

          public List<ContentDataDto> getPrevClauses() {
              return prevClauses;
          }

          public void setPrevClauses(List<ContentDataDto> prevClauses) {
              this.prevClauses = prevClauses;
          }

          public ContentDataDto getRecentClause() {
              return recentClause;
          }

          public void setRecentClause(ContentDataDto recentClause) {
              this.recentClause = recentClause;
          }
      }

      private List<ContentDataDto> convertPrevClauses(List<ContentDataDto> testPrevClauses) {
          if (testPrevClauses == null) {
              return new ArrayList<>();
          }

          return testPrevClauses.stream()
                  .map(
                          prevClause ->
                                  ContentDataDto.builder()
                                          .title(prevClause.getTitle())
                                          .content(prevClause.getContent())
                                          .messages(prevClause.getMessages())
                                          .build())
                  .collect(java.util.stream.Collectors.toList());
      }

      private ContentDataDto convertRecentClause(ContentDataDto testRecentClause) {
          if (testRecentClause == null) {
              return null;
          }

          return ContentDataDto.builder()
                  .title(testRecentClause.getTitle())
                  .content(testRecentClause.getContent())
                  .messages(testRecentClause.getMessages())
                  .build();
      }
}
