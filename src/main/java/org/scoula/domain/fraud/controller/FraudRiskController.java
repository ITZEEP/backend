package org.scoula.domain.fraud.controller;

import java.util.List;

import org.scoula.domain.fraud.dto.request.QuickRiskAnalysisRequest;
import org.scoula.domain.fraud.dto.request.RiskAnalysisRequest;
import org.scoula.domain.fraud.dto.response.DocumentAnalysisResponse;
import org.scoula.domain.fraud.dto.response.LikedHomeResponse;
import org.scoula.domain.fraud.dto.response.RegistryParseResponse;
import org.scoula.domain.fraud.dto.response.RiskAnalysisResponse;
import org.scoula.domain.fraud.dto.response.RiskCheckDetailResponse;
import org.scoula.domain.fraud.dto.response.RiskCheckListResponse;
import org.scoula.domain.fraud.dto.response.TodayRiskCheckResponse;
import org.scoula.global.auth.dto.CustomUserDetails;
import org.scoula.global.common.dto.ApiResponse;
import org.scoula.global.common.dto.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Api(tags = {"사기 위험도 분석 API"})
public interface FraudRiskController {

      @ApiOperation(value = "사기 위험도 분석 기록 목록 조회", notes = "사용자의 사기 위험도 분석 기록 목록을 조회합니다.")
      @GetMapping
      ResponseEntity<PageResponse<RiskCheckListResponse>> getRiskCheckList(
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @ApiParam(value = "페이지 번호", defaultValue = "1") @RequestParam(defaultValue = "1")
                      int page,
              @ApiParam(value = "페이지 크기", defaultValue = "10") @RequestParam(defaultValue = "10")
                      int size);

      @ApiOperation(
              value = "PDF 문서 분석 (OCR)",
              notes = "등기부등본과 건축물대장 PDF 파일을 직접 받아 OCR 분석하여 문서 내용을 추출합니다.")
      @PostMapping(value = "/documents", consumes = "multipart/form-data")
      ResponseEntity<ApiResponse<DocumentAnalysisResponse>> analyzeDocuments(
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @ApiParam(value = "등기부등본 PDF 파일", required = true) @RequestParam("registryFile")
                      MultipartFile registryFile,
              @ApiParam(value = "건축물대장 PDF 파일", required = true) @RequestParam("buildingFile")
                      MultipartFile buildingFile,
              @ApiParam(value = "매물 ID", required = true) @RequestParam Long homeId);

      @ApiOperation(
              value = "사기 위험도 종합 분석 (매물 정보 포함)",
              notes = "매물 정보가 있는 경우 사용합니다. 분석 결과가 DB에 저장됩니다.")
      @PostMapping("/analyze")
      ResponseEntity<ApiResponse<RiskAnalysisResponse>> analyzeRisk(
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @ApiParam(value = "사기 위험도 분석 요청", required = true) @RequestBody
                      RiskAnalysisRequest request);

      @ApiOperation(
              value = "서비스 외 매물 사기 위험도 분석",
              notes = "우리 서비스에 등록되지 않은 외부 매물을 분석합니다. 결과는 DB에 저장되지 않습니다.")
      @PostMapping("/analyze/external")
      ResponseEntity<ApiResponse<RiskAnalysisResponse>> analyzeQuickRisk(
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @ApiParam(value = "서비스 외 매물 사기 위험도 분석 요청", required = true) @RequestBody
                      QuickRiskAnalysisRequest request);

      @ApiOperation(value = "사기 위험도 분석 결과 상세 조회", notes = "특정 사기 위험도 분석 결과의 상세 정보를 조회합니다.")
      @GetMapping("/{riskCheckId}")
      ResponseEntity<ApiResponse<RiskCheckDetailResponse>> getRiskCheckDetail(
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @ApiParam(value = "사기 위험도 체크 ID", required = true, example = "1") @PathVariable
                      Long riskCheckId);

      @ApiOperation(value = "사기 위험도 분석 결과 삭제", notes = "특정 사기 위험도 분석 결과를 삭제합니다.")
      @DeleteMapping("/{riskCheckId}")
      ResponseEntity<ApiResponse<Void>> deleteRiskCheck(
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @ApiParam(value = "사기 위험도 체크 ID", required = true, example = "1") @PathVariable
                      Long riskCheckId);

      @ApiOperation(value = "찜한 매물 목록 조회", notes = "로그인한 사용자가 찜한 매물 목록을 조회합니다.")
      @GetMapping("/liked-homes")
      ResponseEntity<ApiResponse<List<LikedHomeResponse>>> getLikedHomes(
              @AuthenticationPrincipal CustomUserDetails userDetails);

      @ApiOperation(value = "채팅 중인 매물 목록 조회", notes = "로그인한 사용자가 구매자로서 채팅 중인 매물 목록을 최근 대화 순으로 조회합니다.")
      @GetMapping("/chatting-homes")
      ResponseEntity<PageResponse<LikedHomeResponse>> getChattingHomes(
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @ApiParam(value = "페이지 번호", defaultValue = "1") @RequestParam(defaultValue = "1")
                      int page,
              @ApiParam(value = "페이지 크기", defaultValue = "10") @RequestParam(defaultValue = "10")
                      int size);

      @ApiOperation(
              value = "등기부등본 OCR 파싱",
              notes = "등기부등본 PDF 파일을 받아 OCR 파싱하여 DB에 저장하고 파싱된 내용을 반환합니다.")
      @PostMapping(value = "/parse-registry", consumes = "multipart/form-data")
      ResponseEntity<ApiResponse<RegistryParseResponse>> parseRegistryDocument(
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @ApiParam(value = "등기부등본 PDF 파일", required = true) @RequestParam("file")
                      MultipartFile file);

      @ApiOperation(
              value = "오늘 분석한 사기 위험도 요약 정보 조회",
              notes =
                      "매물 ID와 사용자 ID로 오늘 분석한 결과가 있는지 확인하고, 결과를 반환합니다. hasAnalysis 필드로 분석 결과 존재 여부를"
                              + " 확인할 수 있습니다.")
      @GetMapping("/today-check/{homeId}")
      ResponseEntity<ApiResponse<TodayRiskCheckResponse>> getTodayRiskCheckSummary(
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @ApiParam(value = "매물 ID", required = true, example = "1") @PathVariable Long homeId);
}
