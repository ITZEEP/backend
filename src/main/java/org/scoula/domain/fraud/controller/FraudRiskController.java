package org.scoula.domain.fraud.controller;

import java.util.List;

import org.scoula.domain.fraud.dto.request.RiskAnalysisRequest;
import org.scoula.domain.fraud.dto.response.DocumentAnalysisResponse;
import org.scoula.domain.fraud.dto.response.RiskAnalysisResponse;
import org.scoula.domain.fraud.dto.response.RiskCheckDetailResponse;
import org.scoula.domain.fraud.dto.response.RiskCheckListResponse;
import org.scoula.global.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Api(tags = {"사기 위험도 분석 API"})
public interface FraudRiskController {

      @ApiOperation(value = "사기 위험도 분석 기록 목록 조회", notes = "사용자의 사기 위험도 분석 기록 목록을 조회합니다.")
      @GetMapping
      ResponseEntity<ApiResponse<List<RiskCheckListResponse>>> getRiskCheckList(
              @ApiParam(value = "페이지 번호", defaultValue = "0") @RequestParam(defaultValue = "0")
                      int page,
              @ApiParam(value = "페이지 크기", defaultValue = "10") @RequestParam(defaultValue = "10")
                      int size);

      @ApiOperation(
              value = "PDF 문서 분석 (OCR)",
              notes = "등기부등본과 건축물대장 PDF 파일을 직접 받아 OCR 분석하여 문서 내용을 추출합니다.")
      @PostMapping(value = "/documents", consumes = "multipart/form-data")
      ResponseEntity<ApiResponse<DocumentAnalysisResponse>> analyzeDocuments(
              @ApiParam(value = "등기부등본 PDF 파일", required = true) @RequestParam("registryFile")
                      MultipartFile registryFile,
              @ApiParam(value = "건축물대장 PDF 파일", required = true) @RequestParam("buildingFile")
                      MultipartFile buildingFile,
              @ApiParam(value = "매물 ID", required = true) @RequestParam Long homeId);

      @ApiOperation(value = "사기 위험도 종합 분석", notes = "사용자가 확인/수정한 문서 내용을 기반으로 사기 위험도를 종합 분석합니다.")
      @PostMapping("/analyze")
      ResponseEntity<ApiResponse<RiskAnalysisResponse>> analyzeRisk(
              @ApiParam(value = "사기 위험도 분석 요청", required = true) @RequestBody
                      RiskAnalysisRequest request);

      @ApiOperation(value = "사기 위험도 분석 결과 상세 조회", notes = "특정 사기 위험도 분석 결과의 상세 정보를 조회합니다.")
      @GetMapping("/{riskCheckId}")
      ResponseEntity<ApiResponse<RiskCheckDetailResponse>> getRiskCheckDetail(
              @ApiParam(value = "사기 위험도 체크 ID", required = true, example = "1") @PathVariable
                      Long riskCheckId);

      @ApiOperation(value = "사기 위험도 분석 결과 삭제", notes = "특정 사기 위험도 분석 결과를 삭제합니다.")
      @DeleteMapping("/{riskCheckId}")
      ResponseEntity<ApiResponse<Void>> deleteRiskCheck(
              @ApiParam(value = "사기 위험도 체크 ID", required = true, example = "1") @PathVariable
                      Long riskCheckId);
}
