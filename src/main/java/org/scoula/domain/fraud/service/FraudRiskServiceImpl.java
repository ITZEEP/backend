package org.scoula.domain.fraud.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.scoula.domain.fraud.dto.ai.FraudRiskCheckDto;
import org.scoula.domain.fraud.dto.common.BuildingDocumentDto;
import org.scoula.domain.fraud.dto.common.RegistryDocumentDto;
import org.scoula.domain.fraud.dto.request.RiskAnalysisRequest;
import org.scoula.domain.fraud.dto.response.DocumentAnalysisResponse;
import org.scoula.domain.fraud.dto.response.LikedHomeResponse;
import org.scoula.domain.fraud.dto.response.RiskAnalysisResponse;
import org.scoula.domain.fraud.dto.response.RiskCheckDetailResponse;
import org.scoula.domain.fraud.dto.response.RiskCheckListResponse;
import org.scoula.domain.fraud.enums.AnalysisStatus;
import org.scoula.domain.fraud.enums.RiskType;
import org.scoula.domain.fraud.exception.FraudRiskException;
import org.scoula.domain.fraud.mapper.FraudRiskMapper;
import org.scoula.domain.fraud.mapper.HomeLikeMapper;
import org.scoula.domain.fraud.vo.RiskCheckDetailVO;
import org.scoula.domain.fraud.vo.RiskCheckVO;
import org.scoula.global.common.dto.PageRequest;
import org.scoula.global.common.dto.PageResponse;
import org.scoula.global.file.service.S3ServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
@Transactional(readOnly = true)
public class FraudRiskServiceImpl implements FraudRiskService {

      private final FraudRiskMapper fraudRiskMapper;
      private final HomeLikeMapper homeLikeMapper;
      private final S3ServiceInterface s3Service;
      private final ObjectMapper objectMapper;
      private final AiFraudAnalyzerService aiFraudAnalyzerService;

      // 허용된 파일 확장자
      private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("pdf", "PDF");
      private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

      @Override
      @Transactional
      public DocumentAnalysisResponse analyzeDocuments(
              Long userId, MultipartFile registryFile, MultipartFile buildingFile, Long homeId) {
          log.info("문서 분석 시작 - homeId: {}", homeId);

          // 1. 파일 유효성 검증
          validateFile(registryFile, "등기부등본");
          validateFile(buildingFile, "건축물대장");

          // 2. 매물 존재 여부 확인 (homeId가 있는 경우에만)
          if (homeId != null && !fraudRiskMapper.existsHome(homeId)) {
              throw new FraudRiskException("존재하지 않는 매물입니다.", "RISK4001");
          }

          LocalDateTime startTime = LocalDateTime.now();

          try {
              // 3. S3에 파일 업로드
              String registryFileName =
                      "fraud/"
                              + userId
                              + "/registry_"
                              + homeId
                              + "_"
                              + System.currentTimeMillis()
                              + ".pdf";
              String buildingFileName =
                      "fraud/"
                              + userId
                              + "/building_"
                              + homeId
                              + "_"
                              + System.currentTimeMillis()
                              + ".pdf";

              String registryFileKey = s3Service.uploadFile(registryFile, registryFileName);
              String buildingFileKey = s3Service.uploadFile(buildingFile, buildingFileName);

              // 4. S3 URL 가져오기
              String registryFileUrl = s3Service.getFileUrl(registryFileKey);
              String buildingFileUrl = s3Service.getFileUrl(buildingFileKey);

              // AI OCR 서비스 호출 (실제 파일로)
              RegistryDocumentDto registryDoc =
                      aiFraudAnalyzerService.parseRegistryDocument(registryFile);
              BuildingDocumentDto buildingDoc =
                      aiFraudAnalyzerService.parseBuildingDocument(buildingFile);

              // 5. 응답 생성
              LocalDateTime endTime = LocalDateTime.now();
              double processingTime =
                      java.time.Duration.between(startTime, endTime).toMillis() / 1000.0;

              return DocumentAnalysisResponse.builder()
                      .homeId(homeId)
                      .registryDocument(registryDoc)
                      .buildingDocument(buildingDoc)
                      .registryAnalysisStatus(AnalysisStatus.SUCCESS.name())
                      .buildingAnalysisStatus(AnalysisStatus.SUCCESS.name())
                      .registryFileUrl(registryFileUrl)
                      .buildingFileUrl(buildingFileUrl)
                      .registryAnalyzedAt(startTime)
                      .buildingAnalyzedAt(endTime)
                      .processingTime(processingTime)
                      .build();

          } catch (FraudRiskException e) {
              // FraudRiskException은 그대로 다시 던지기 (에러 코드 보존)
              throw e;
          } catch (Exception e) {
              log.error("문서 분석 중 예상치 못한 오류 발생", e);
              throw new FraudRiskException("문서 분석 중 오류가 발생했습니다: " + e.getMessage(), "RISK5000");
          }
      }

      @Override
      @Transactional
      public RiskAnalysisResponse analyzeRisk(Long userId, RiskAnalysisRequest request) {
          log.info("위험도 분석 시작 - homeId: {}", request.getHomeId());

          try {
              // 1. risk_check 레코드 생성
              RiskCheckVO riskCheck =
                      RiskCheckVO.builder()
                              .userId(userId)
                              .homeId(request.getHomeId())
                              .riskType(RiskType.SAFE) // 초기값 설정 (분석 후 업데이트)
                              .registryFileUrl(request.getRegistryFileUrl())
                              .buildingFileUrl(request.getBuildingFileUrl())
                              .registryFileDate(LocalDateTime.now())
                              .buildingFileDate(LocalDateTime.now())
                              .build();

              fraudRiskMapper.insertRiskCheck(riskCheck);

              // 2. AI 분석 서비스 호출
              FraudRiskCheckDto.Response aiResponse = null;
              RiskType riskType = RiskType.WARN; // 기본값을 WARN으로 설정

              try {
                  // AI 서버에 분석 요청
                  aiResponse = aiFraudAnalyzerService.analyzeFraudRisk(userId, request);
                  riskType = aiFraudAnalyzerService.determineRiskType(aiResponse);

                  log.info(
                          "AI 분석 결과 - riskType: {}, riskScore: {}",
                          riskType,
                          aiResponse.getRiskScore());
              } catch (Exception e) {
                  log.error("AI 분석 실패", e);
                  throw new FraudRiskException("AI 분석 중 오류가 발생했습니다: " + e.getMessage());
              }

              // 3. risk_check 업데이트
              riskCheck.setRiskType(riskType);
              fraudRiskMapper.updateRiskCheck(riskCheck);

              // 4. 기존 상세 분석 결과 삭제 (중복 방지)
              fraudRiskMapper.deleteRiskCheckDetail(riskCheck.getRiskckId());

              // 5. AI 분석 결과를 risk_check_detail에 저장
              if (aiResponse != null && aiResponse.getAnalysisResults() != null) {
                  Map<String, Object> analysisResults = aiResponse.getAnalysisResults();

                  // AI 분석 결과의 각 항목을 risk_check_detail로 저장
                  // 변환된 구조: {"그룹명": {"항목명": {"title": "...", "content": "..."}}}
                  for (Map.Entry<String, Object> groupEntry : analysisResults.entrySet()) {
                      String title1 = groupEntry.getKey(); // 그룹명 (예: "권리관계 정보")
                      Object groupValue = groupEntry.getValue();

                      if (groupValue instanceof Map) {
                          @SuppressWarnings("unchecked")
                          Map<String, Object> groupItems = (Map<String, Object>) groupValue;

                          // 각 그룹 내의 항목들 처리
                          for (Map.Entry<String, Object> itemEntry : groupItems.entrySet()) {
                              Object itemValue = itemEntry.getValue();

                              if (itemValue instanceof Map) {
                                  @SuppressWarnings("unchecked")
                                  Map<String, Object> itemDetails = (Map<String, Object>) itemValue;

                                  String title2 = itemDetails.getOrDefault("title", "").toString();
                                  String content = itemDetails.getOrDefault("content", "").toString();

                                  RiskCheckDetailVO detail =
                                          RiskCheckDetailVO.builder()
                                                  .riskckId(riskCheck.getRiskckId())
                                                  .title1(title1)
                                                  .title2(title2)
                                                  .content(content)
                                                  .build();

                                  try {
                                      fraudRiskMapper.insertRiskCheckDetail(detail);
                                      log.debug(
                                              "상세 분석 결과 저장 성공 - title1: {}, title2: {}",
                                              title1,
                                              title2);
                                  } catch (Exception e) {
                                      log.warn(
                                              "상세 분석 결과 저장 실패 - title1: {}, title2: {}, error: {}",
                                              title1,
                                              title2,
                                              e.getMessage());
                                  }
                              }
                          }
                      }
                  }
              }

              // 추천사항이 있으면 추가로 저장
              if (aiResponse != null
                      && aiResponse.getRecommendations() != null
                      && !aiResponse.getRecommendations().isEmpty()) {
                  String recommendations = String.join("\n", aiResponse.getRecommendations());
                  RiskCheckDetailVO recommendDetail =
                          RiskCheckDetailVO.builder()
                                  .riskckId(riskCheck.getRiskckId())
                                  .title1("추천사항")
                                  .title2("AI 분석 기반 추천")
                                  .content(recommendations)
                                  .build();

                  try {
                      fraudRiskMapper.insertRiskCheckDetail(recommendDetail);
                  } catch (Exception e) {
                      log.warn("추천사항 저장 실패: {}", e.getMessage());
                  }
              }

              // 6. 저장된 상세 분석 결과 조회 및 그룹화
              List<RiskCheckDetailVO> savedDetails =
                      fraudRiskMapper.selectRiskCheckDetailByRiskCheckId(riskCheck.getRiskckId());

              List<RiskCheckDetailResponse.DetailGroup> detailGroups = new ArrayList<>();
              if (savedDetails != null && !savedDetails.isEmpty()) {
                  Map<String, List<RiskCheckDetailVO>> groupedDetails =
                          savedDetails.stream()
                                  .collect(
                                          Collectors.groupingBy(
                                                  RiskCheckDetailVO::getTitle1,
                                                  LinkedHashMap::new,
                                                  Collectors.toList()));

                  for (Map.Entry<String, List<RiskCheckDetailVO>> entry : groupedDetails.entrySet()) {
                      RiskCheckDetailResponse.DetailGroup group =
                              RiskCheckDetailResponse.DetailGroup.builder()
                                      .title(entry.getKey())
                                      .items(
                                              entry.getValue().stream()
                                                      .map(
                                                              detail ->
                                                                      RiskCheckDetailResponse
                                                                              .DetailItem.builder()
                                                                              .title(
                                                                                      detail
                                                                                              .getTitle2())
                                                                              .content(
                                                                                      detail
                                                                                              .getContent())
                                                                              .build())
                                                      .collect(Collectors.toList()))
                                      .build();
                      detailGroups.add(group);
                  }
              }

              // 7. 응답 반환
              return RiskAnalysisResponse.builder()
                      .riskCheckId(riskCheck.getRiskckId())
                      .riskType(riskType)
                      .analyzedAt(LocalDateTime.now())
                      .detailGroups(detailGroups)
                      .build();

          } catch (FraudRiskException e) {
              // FraudRiskException은 그대로 다시 던지기 (에러 코드 보존)
              throw e;
          } catch (Exception e) {
              log.error("위험도 분석 실패", e);
              throw new FraudRiskException("위험도 분석 중 오류가 발생했습니다: " + e.getMessage(), "RISK5003");
          }
      }

      @Override
      public PageResponse<RiskCheckListResponse> getRiskCheckList(
              Long userId, PageRequest pageRequest) {
          // 정렬 기본값 설정
          if (pageRequest.getSort() == null || pageRequest.getSort().isEmpty()) {
              pageRequest.setSort("checkedAt");
              pageRequest.setDirection("DESC");
          }

          List<RiskCheckListResponse> list =
                  fraudRiskMapper.selectRiskChecksByUserId(userId, pageRequest);
          long totalElements = fraudRiskMapper.countRiskChecksByUserId(userId);

          return PageResponse.of(list, pageRequest, totalElements);
      }

      @Override
      public RiskCheckDetailResponse getRiskCheckDetail(Long userId, Long riskCheckId) {
          // 권한 확인
          if (!fraudRiskMapper.isOwnerOfRiskCheck(riskCheckId, userId)) {
              throw new FraudRiskException("해당 위험도 체크 결과에 대한 권한이 없습니다.");
          }

          RiskCheckDetailResponse response =
                  fraudRiskMapper.selectRiskCheckDetailResponse(riskCheckId);
          if (response == null) {
              throw new FraudRiskException("위험도 체크 결과를 찾을 수 없습니다.");
          }

          // 거래 타입 결정 (leaseType 기반)
          if ("JEONSE".equals(response.getLeaseType())) {
              response.setTransactionType("전세");
          } else if ("WOLSE".equals(response.getLeaseType())) {
              response.setTransactionType("월세");
          } else {
              // leaseType이 null이거나 다른 값인 경우 매매로 간주
              response.setTransactionType("매매");
          }

          // 매물 이미지 URL은 DB에서 조회된 것을 사용 (FraudRiskMapper에서 설정됨)

          // RiskCheckDetail 조회 및 title1별로 그룹화
          List<RiskCheckDetailVO> details =
                  fraudRiskMapper.selectRiskCheckDetailByRiskCheckId(riskCheckId);
          if (details != null && !details.isEmpty()) {
              Map<String, List<RiskCheckDetailVO>> groupedDetails =
                      details.stream()
                              .collect(
                                      Collectors.groupingBy(
                                              RiskCheckDetailVO::getTitle1,
                                              LinkedHashMap::new,
                                              Collectors.toList()));

              List<RiskCheckDetailResponse.DetailGroup> detailGroups = new ArrayList<>();
              for (Map.Entry<String, List<RiskCheckDetailVO>> entry : groupedDetails.entrySet()) {
                  RiskCheckDetailResponse.DetailGroup group =
                          RiskCheckDetailResponse.DetailGroup.builder()
                                  .title(entry.getKey())
                                  .items(
                                          entry.getValue().stream()
                                                  .map(
                                                          detail ->
                                                                  RiskCheckDetailResponse.DetailItem
                                                                          .builder()
                                                                          .title(detail.getTitle2())
                                                                          .content(
                                                                                  detail.getContent())
                                                                          .build())
                                                  .collect(Collectors.toList()))
                                  .build();
                  detailGroups.add(group);
              }
              response.setDetailGroups(detailGroups);
          }

          return response;
      }

      @Override
      @Transactional
      public void deleteRiskCheck(Long userId, Long riskCheckId) {
          // 권한 확인
          if (!fraudRiskMapper.isOwnerOfRiskCheck(riskCheckId, userId)) {
              throw new FraudRiskException("해당 위험도 체크 결과에 대한 삭제 권한이 없습니다.");
          }

          // 상세 정보 먼저 삭제
          fraudRiskMapper.deleteRiskCheckDetail(riskCheckId);

          // 메인 정보 삭제
          int deleted = fraudRiskMapper.deleteRiskCheck(riskCheckId);
          if (deleted == 0) {
              throw new FraudRiskException("위험도 체크 결과 삭제에 실패했습니다.");
          }
      }

      // ========== Private Helper Methods ==========

      private void validateFile(MultipartFile file, String fileType) {
          if (file == null || file.isEmpty()) {
              throw new FraudRiskException(fileType + " 파일이 없습니다.", "RISK4002");
          }

          if (file.getSize() > MAX_FILE_SIZE) {
              throw new FraudRiskException(fileType + " 파일 크기가 10MB를 초과합니다.", "RISK4003");
          }

          String filename = file.getOriginalFilename();
          if (filename == null || !hasValidExtension(filename)) {
              throw new FraudRiskException(fileType + "은(는) PDF 파일만 업로드 가능합니다.", "RISK4004");
          }
      }

      private boolean hasValidExtension(String filename) {
          String extension = filename.substring(filename.lastIndexOf(".") + 1);
          return ALLOWED_EXTENSIONS.contains(extension);
      }

      @Override
      public List<LikedHomeResponse> getLikedHomes(Long userId) {
          log.info("찜한 매물 목록 조회 - userId: {}", userId);
          return homeLikeMapper.selectLikedHomesByUserId(userId);
      }

      @Override
      public PageResponse<LikedHomeResponse> getChattingHomes(Long userId, PageRequest pageRequest) {
          log.info(
                  "채팅 중인 매물 목록 조회 - userId: {}, page: {}, size: {}",
                  userId,
                  pageRequest.getPage(),
                  pageRequest.getSize());

          // 정렬 기본값 설정
          if (pageRequest.getSort() == null || pageRequest.getSort().isEmpty()) {
              pageRequest.setSort("lastMessageAt");
              pageRequest.setDirection("DESC");
          }

          List<LikedHomeResponse> list =
                  homeLikeMapper.selectChattingHomesByUserId(userId, pageRequest);
          long totalElements = homeLikeMapper.countChattingHomesByUserId(userId);

          return PageResponse.of(list, pageRequest, totalElements);
      }

      private String getTitleByRiskType(RiskType riskType) {
          switch (riskType) {
              case SAFE:
                  return "✅ 안전한 매물";
              case WARN:
                  return "⚠️ 주의 필요";
              case DANGER:
                  return "🚨 위험 매물";
              default:
                  return "분석 완료";
          }
      }
}
