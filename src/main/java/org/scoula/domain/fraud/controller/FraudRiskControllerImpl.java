package org.scoula.domain.fraud.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.scoula.domain.fraud.dto.common.BuildingInfoDto;
import org.scoula.domain.fraud.dto.common.DetailedAnalysisDto;
import org.scoula.domain.fraud.dto.request.RiskAnalysisRequest;
import org.scoula.domain.fraud.dto.response.DocumentAnalysisResponse;
import org.scoula.domain.fraud.dto.response.RiskAnalysisResponse;
import org.scoula.domain.fraud.dto.response.RiskCheckDetailResponse;
import org.scoula.domain.fraud.dto.response.RiskCheckListResponse;
import org.scoula.domain.fraud.enums.RiskType;
import org.scoula.global.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequestMapping("/api/fraud-risk")
@RequiredArgsConstructor
@Log4j2
public class FraudRiskControllerImpl implements FraudRiskController {

      @Override
      @GetMapping
      public ResponseEntity<ApiResponse<List<RiskCheckListResponse>>> getRiskCheckList(
              @RequestParam(defaultValue = "0") int page,
              @RequestParam(defaultValue = "10") int size) {

          log.info("사기 위험도 분석 기록 목록 조회 - page: {}, size: {}", page, size);

          List<RiskCheckListResponse> riskCheckList = new ArrayList<>();

          return ResponseEntity.ok(ApiResponse.success(riskCheckList));
      }

      @Override
      @PostMapping(value = "/documents", consumes = "multipart/form-data")
      public ResponseEntity<ApiResponse<DocumentAnalysisResponse>> analyzeDocuments(
              @RequestParam("registryFile") MultipartFile registryFile,
              @RequestParam("buildingFile") MultipartFile buildingFile,
              @RequestParam Long homeId) {

          log.info(
                  "PDF 문서 분석 (OCR) 요청 - 매물 ID: {}, 등기부등본: {}, 건축물대장: {}",
                  homeId,
                  registryFile.getOriginalFilename(),
                  buildingFile.getOriginalFilename());

          // TODO: 실제 OCR 서비스로 PDF 분석 후 내용 추출
          // 현재는 모의 데이터 반환
          DocumentAnalysisResponse response =
                  DocumentAnalysisResponse.builder()
                          .homeId(homeId)
                          .registryAnalysisStatus("SUCCESS")
                          .buildingAnalysisStatus("SUCCESS")
                          .registryAnalyzedAt(LocalDateTime.now())
                          .buildingAnalyzedAt(LocalDateTime.now())
                          .processingTime(3.5)
                          .build();

          return ResponseEntity.ok(ApiResponse.success(response));
      }

      @Override
      @PostMapping("/analyze")
      public ResponseEntity<ApiResponse<RiskAnalysisResponse>> analyzeRisk(
              @RequestBody RiskAnalysisRequest request) {

          log.info("사기 위험도 종합 분석 요청 - 매물 ID: {}", request.getHomeId());

          // TODO: 실제 위험도 분석 서비스 구현 후 연결
          BuildingInfoDto buildingInfo =
                  BuildingInfoDto.builder()
                          .address("서울특별시 강남구 테헤란로 123")
                          .detailAddress("101동 1503호")
                          .buildingType("아파트")
                          .transactionType("전세")
                          .price(500000000L)
                          .build();

          DetailedAnalysisDto detailedAnalysis =
                  DetailedAnalysisDto.builder()
                          .basicInfo(
                                  DetailedAnalysisDto.BasicInfoAnalysis.builder()
                                          .ownerInfoMatch("등기부등본의 소유자와 임대인 정보가 일치합니다.")
                                          .addressMatch("등기부등본과 건축물대장의 주소가 정확히 일치합니다.")
                                          .build())
                          .buildingInfo(
                                  DetailedAnalysisDto.BuildingInfoAnalysis.builder()
                                          .buildingPurposeCheck("건축물대장상 용도는 아파트로 주거용으로 적합합니다.")
                                          .floorAreaInfo("15층 건물 중 10층에 위치하며, 전용면적 84㎡로 표기되어 있습니다.")
                                          .build())
                          .rightsInfo(
                                  DetailedAnalysisDto.RightsAnalysis.builder()
                                          .mortgageStatus("현재 근저당 설정이 없어 안전합니다.")
                                          .realPriceRatio("시세 대비 95% 수준으로 적정한 가격대입니다.")
                                          .build())
                          .legalRisk(
                                  DetailedAnalysisDto.LegalRiskAnalysis.builder()
                                          .legalProceedingsStatus(
                                                  "현재 진행 중인 법적 분쟁이나 강제집행 절차가 없어 안전합니다.")
                                          .violationBuildingStatus("적법하게 건축된 건물로 위반사항이 없습니다.")
                                          .build())
                          .build();

          RiskAnalysisResponse response =
                  RiskAnalysisResponse.builder()
                          .buildingInfo(buildingInfo)
                          .riskType(RiskType.SAFE)
                          .detailedAnalysis(detailedAnalysis)
                          .build();

          return ResponseEntity.ok(ApiResponse.success(response));
      }

      @Override
      @GetMapping("/{riskCheckId}")
      public ResponseEntity<ApiResponse<RiskCheckDetailResponse>> getRiskCheckDetail(
              @PathVariable Long riskCheckId) {

          log.info("사기 위험도 분석 결과 상세 조회 - riskCheckId: {}", riskCheckId);

          // TODO: 실제 서비스 구현 후 연결
          // TODO: 실제 서비스 구현 후 연결
          BuildingInfoDto buildingInfo =
                  BuildingInfoDto.builder()
                          .address("서울특별시 강남구 테헤란로 123")
                          .detailAddress("101동 1503호")
                          .buildingType("아파트")
                          .transactionType("전세")
                          .price(500000000L)
                          .build();

          DetailedAnalysisDto detailedAnalysis =
                  DetailedAnalysisDto.builder()
                          .basicInfo(
                                  DetailedAnalysisDto.BasicInfoAnalysis.builder()
                                          .ownerInfoMatch("등기부등본의 소유자와 임대인 정보가 일치합니다.")
                                          .addressMatch("등기부등본과 건축물대장의 주소가 정확히 일치합니다.")
                                          .build())
                          .buildingInfo(
                                  DetailedAnalysisDto.BuildingInfoAnalysis.builder()
                                          .buildingPurposeCheck("건축물대장상 용도는 아파트로 주거용으로 적합합니다.")
                                          .floorAreaInfo("15층 건물 중 10층에 위치하며, 전용면적 84㎡로 표기되어 있습니다.")
                                          .build())
                          .rightsInfo(
                                  DetailedAnalysisDto.RightsAnalysis.builder()
                                          .mortgageStatus("현재 근저당 설정이 없어 안전합니다.")
                                          .realPriceRatio("시세 대비 95% 수준으로 적정한 가격대입니다.")
                                          .build())
                          .legalRisk(
                                  DetailedAnalysisDto.LegalRiskAnalysis.builder()
                                          .legalProceedingsStatus(
                                                  "현재 진행 중인 법적 분쟁이나 강제집행 절차가 없어 안전합니다.")
                                          .violationBuildingStatus("적법하게 건축된 건물로 위반사항이 없습니다.")
                                          .build())
                          .build();

          RiskCheckDetailResponse response =
                  RiskCheckDetailResponse.builder()
                          .buildingInfo(buildingInfo)
                          .riskType(RiskType.SAFE)
                          .detailedAnalysis(detailedAnalysis)
                          .build();

          return ResponseEntity.ok(ApiResponse.success(response));
      }

      @Override
      @DeleteMapping("/{riskCheckId}")
      public ResponseEntity<ApiResponse<Void>> deleteRiskCheck(@PathVariable Long riskCheckId) {

          log.info("사기 위험도 분석 결과 삭제 - riskCheckId: {}", riskCheckId);

          // TODO: 실제 서비스 구현 후 연결
          // 서비스에서 삭제 처리 후 성공 반환

          return ResponseEntity.ok(ApiResponse.success(null));
      }
}
