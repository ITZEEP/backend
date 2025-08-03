package org.scoula.domain.fraud.service;

import java.util.List;

import org.scoula.domain.fraud.dto.request.ExternalRiskAnalysisRequest;
import org.scoula.domain.fraud.dto.request.RiskAnalysisRequest;
import org.scoula.domain.fraud.dto.response.DocumentAnalysisResponse;
import org.scoula.domain.fraud.dto.response.LikedHomeResponse;
import org.scoula.domain.fraud.dto.response.RiskAnalysisResponse;
import org.scoula.domain.fraud.dto.response.RiskCheckDetailResponse;
import org.scoula.domain.fraud.dto.response.RiskCheckListResponse;
import org.scoula.domain.fraud.dto.response.RiskCheckSummaryResponse;
import org.scoula.global.common.dto.PageRequest;
import org.scoula.global.common.dto.PageResponse;
import org.springframework.web.multipart.MultipartFile;

public interface FraudRiskService {

      /**
       * PDF 문서 분석 (OCR)
       *
       * @param userId 사용자 ID
       * @param registryFile 등기부등본 PDF 파일
       * @param buildingFile 건축물대장 PDF 파일
       * @param homeId 매물 ID
       * @return 문서 분석 결과
       */
      DocumentAnalysisResponse analyzeDocuments(
              Long userId, MultipartFile registryFile, MultipartFile buildingFile, Long homeId);

      /**
       * 사기 위험도 종합 분석 (매물 정보 포함)
       *
       * @param userId 사용자 ID
       * @param request 분석 요청 정보
       * @return 위험도 분석 결과
       */
      RiskAnalysisResponse analyzeRisk(Long userId, RiskAnalysisRequest request);

      /**
       * 서비스 외 매물 사기 위험도 분석
       *
       * @param userId 사용자 ID
       * @param request 서비스 외 매물 분석 요청 정보
       * @return 위험도 분석 결과
       */
      RiskAnalysisResponse analyzeExternalRisk(Long userId, ExternalRiskAnalysisRequest request);

      /**
       * 사용자의 위험도 체크 목록 조회
       *
       * @param userId 사용자 ID
       * @param pageRequest 페이징 정보
       * @return 위험도 체크 목록
       */
      PageResponse<RiskCheckListResponse> getRiskCheckList(Long userId, PageRequest pageRequest);

      /**
       * 위험도 체크 상세 조회
       *
       * @param userId 사용자 ID
       * @param riskCheckId 위험도 체크 ID
       * @return 상세 정보
       */
      RiskCheckDetailResponse getRiskCheckDetail(Long userId, Long riskCheckId);

      /**
       * 위험도 체크 삭제
       *
       * @param userId 사용자 ID
       * @param riskCheckId 위험도 체크 ID
       */
      void deleteRiskCheck(Long userId, Long riskCheckId);

      /**
       * 찜한 매물 목록 조회
       *
       * @param userId 사용자 ID
       * @return 찜한 매물 목록
       */
      List<LikedHomeResponse> getLikedHomes(Long userId);

      /**
       * 채팅 중인 매물 목록 조회 (구매자로서)
       *
       * @param userId 사용자 ID
       * @param pageRequest 페이징 정보
       * @return 채팅 중인 매물 목록
       */
      PageResponse<LikedHomeResponse> getChattingHomes(Long userId, PageRequest pageRequest);

      /**
       * 오늘 분석한 사기 위험도 요약 정보 조회
       *
       * @param userId 사용자 ID
       * @param homeId 매물 ID
       * @return 오늘 분석한 위험도 체크 요약 정보 (없으면 null)
       */
      RiskCheckSummaryResponse getTodayRiskCheckSummary(Long userId, Long homeId);
}
