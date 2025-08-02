package org.scoula.domain.fraud.controller;

import java.io.IOException;
import java.util.List;

import org.scoula.domain.fraud.dto.request.QuickRiskAnalysisRequest;
import org.scoula.domain.fraud.dto.request.RiskAnalysisRequest;
import org.scoula.domain.fraud.dto.response.DocumentAnalysisResponse;
import org.scoula.domain.fraud.dto.response.LikedHomeResponse;
import org.scoula.domain.fraud.dto.response.RegistryParseResponse;
import org.scoula.domain.fraud.dto.response.RiskAnalysisResponse;
import org.scoula.domain.fraud.dto.response.RiskCheckDetailResponse;
import org.scoula.domain.fraud.dto.response.RiskCheckListResponse;
import org.scoula.domain.fraud.dto.response.RiskCheckSummaryResponse;
import org.scoula.domain.fraud.dto.response.TodayRiskCheckResponse;
import org.scoula.domain.fraud.exception.FraudErrorCode;
import org.scoula.domain.fraud.exception.FraudRiskException;
import org.scoula.domain.fraud.service.AiDocumentAnalyzerService;
import org.scoula.domain.fraud.service.FraudRiskService;
import org.scoula.global.auth.dto.CustomUserDetails;
import org.scoula.global.common.dto.ApiResponse;
import org.scoula.global.common.dto.PageRequest;
import org.scoula.global.common.dto.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/fraud-risk")
@RequiredArgsConstructor
public class FraudRiskControllerImpl implements FraudRiskController {

      private final FraudRiskService fraudRiskService;
      private final AiDocumentAnalyzerService aiDocumentAnalyzerService;

      @Override
      @GetMapping
      public ResponseEntity<PageResponse<RiskCheckListResponse>> getRiskCheckList(
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @RequestParam(defaultValue = "1") int page,
              @RequestParam(defaultValue = "10") int size) {

          Long userId = userDetails.getUserId();
          PageRequest pageRequest =
                  PageRequest.builder()
                          .page(page)
                          .size(size)
                          .sort("checkedAt")
                          .direction("DESC")
                          .build();

          PageResponse<RiskCheckListResponse> pageResponse =
                  fraudRiskService.getRiskCheckList(userId, pageRequest);

          return ResponseEntity.ok(pageResponse);
      }

      @Override
      @PostMapping(value = "/documents", consumes = "multipart/form-data")
      public ResponseEntity<ApiResponse<DocumentAnalysisResponse>> analyzeDocuments(
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @RequestParam("registryFile") MultipartFile registryFile,
              @RequestParam("buildingFile") MultipartFile buildingFile,
              @RequestParam(required = false) Long homeId) {

          Long userId = userDetails.getUserId();
          DocumentAnalysisResponse response =
                  fraudRiskService.analyzeDocuments(userId, registryFile, buildingFile, homeId);

          return ResponseEntity.ok(ApiResponse.success(response));
      }

      @Override
      @PostMapping("/analyze")
      public ResponseEntity<ApiResponse<RiskAnalysisResponse>> analyzeRisk(
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @RequestBody RiskAnalysisRequest request) {

          if (request.getRegistryDocument() == null) {
              throw new FraudRiskException(FraudErrorCode.MISSING_REQUIRED_FIELDS, "등기부등본 정보는 필수입니다");
          }

          if (request.getBuildingDocument() == null) {
              throw new FraudRiskException(FraudErrorCode.MISSING_REQUIRED_FIELDS, "건축물대장 정보는 필수입니다");
          }

          Long userId = userDetails.getUserId();
          RiskAnalysisResponse response = fraudRiskService.analyzeRisk(userId, request);

          return ResponseEntity.ok(ApiResponse.success(response, "사기 위험도 분석이 완료되었습니다."));
      }

      @Override
      @GetMapping("/{riskCheckId}")
      public ResponseEntity<ApiResponse<RiskCheckDetailResponse>> getRiskCheckDetail(
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @PathVariable Long riskCheckId) {

          Long userId = userDetails.getUserId();
          RiskCheckDetailResponse response = fraudRiskService.getRiskCheckDetail(userId, riskCheckId);

          return ResponseEntity.ok(ApiResponse.success(response));
      }

      @Override
      @DeleteMapping("/{riskCheckId}")
      public ResponseEntity<ApiResponse<Void>> deleteRiskCheck(
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @PathVariable Long riskCheckId) {

          Long userId = userDetails.getUserId();
          fraudRiskService.deleteRiskCheck(userId, riskCheckId);

          return ResponseEntity.ok(ApiResponse.success(null));
      }

      @Override
      @GetMapping("/liked-homes")
      public ResponseEntity<ApiResponse<List<LikedHomeResponse>>> getLikedHomes(
              @AuthenticationPrincipal CustomUserDetails userDetails) {

          Long userId = userDetails.getUserId();
          List<LikedHomeResponse> likedHomes = fraudRiskService.getLikedHomes(userId);

          return ResponseEntity.ok(ApiResponse.success(likedHomes));
      }

      @Override
      @GetMapping("/chatting-homes")
      public ResponseEntity<PageResponse<LikedHomeResponse>> getChattingHomes(
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @RequestParam(defaultValue = "1") int page,
              @RequestParam(defaultValue = "10") int size) {

          Long userId = userDetails.getUserId();
          PageRequest pageRequest =
                  PageRequest.builder()
                          .page(page)
                          .size(size)
                          .sort("lastMessageAt")
                          .direction("DESC")
                          .build();

          PageResponse<LikedHomeResponse> pageResponse =
                  fraudRiskService.getChattingHomes(userId, pageRequest);

          return ResponseEntity.ok(pageResponse);
      }

      @Override
      @PostMapping(value = "/parse-registry", consumes = "multipart/form-data")
      public ResponseEntity<ApiResponse<RegistryParseResponse>> parseRegistryDocument(
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @RequestParam("file") MultipartFile file) {

          Long userId = userDetails.getUserId();
          try {
              RegistryParseResponse response =
                      aiDocumentAnalyzerService.analyzeAndParseRegistryDocument(file);
              return ResponseEntity.ok(ApiResponse.success(response, "등기부등본 파싱이 완료되었습니다."));
          } catch (IOException e) {
              throw new FraudRiskException(
                      FraudErrorCode.DOCUMENT_PROCESSING_FAILED,
                      "등기부등본 파싱 중 오류가 발생했습니다: " + e.getMessage());
          }
      }

      @Override
      @PostMapping("/analyze/external")
      public ResponseEntity<ApiResponse<RiskAnalysisResponse>> analyzeQuickRisk(
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @RequestBody QuickRiskAnalysisRequest request) {

          if (request.getRegistryDocument() == null) {
              throw new FraudRiskException(FraudErrorCode.MISSING_REQUIRED_FIELDS, "등기부등본 정보는 필수입니다");
          }

          if (request.getBuildingDocument() == null) {
              throw new FraudRiskException(FraudErrorCode.MISSING_REQUIRED_FIELDS, "건축물대장 정보는 필수입니다");
          }

          Long userId = userDetails.getUserId();
          RiskAnalysisResponse response = fraudRiskService.analyzeQuickRisk(userId, request);

          return ResponseEntity.ok(ApiResponse.success(response, "사기 위험도 분석이 완료되었습니다."));
      }

      @Override
      @GetMapping("/today-check/{homeId}")
      public ResponseEntity<ApiResponse<TodayRiskCheckResponse>> getTodayRiskCheckSummary(
              @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long homeId) {

          Long userId = userDetails.getUserId();
          RiskCheckSummaryResponse summary =
                  fraudRiskService.getTodayRiskCheckSummary(userId, homeId);

          TodayRiskCheckResponse response =
                  summary != null
                          ? TodayRiskCheckResponse.withAnalysis(summary)
                          : TodayRiskCheckResponse.noAnalysis();

          return ResponseEntity.ok(ApiResponse.success(response));
      }
}
