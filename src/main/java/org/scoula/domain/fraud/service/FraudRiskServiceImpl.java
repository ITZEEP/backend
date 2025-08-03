package org.scoula.domain.fraud.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.scoula.domain.fraud.dto.ai.FraudRiskCheckDto;
import org.scoula.domain.fraud.dto.common.BuildingDocumentDto;
import org.scoula.domain.fraud.dto.common.RegistryDocumentDto;
import org.scoula.domain.fraud.dto.request.ExternalRiskAnalysisRequest;
import org.scoula.domain.fraud.dto.request.RiskAnalysisRequest;
import org.scoula.domain.fraud.dto.response.DocumentAnalysisResponse;
import org.scoula.domain.fraud.dto.response.LikedHomeResponse;
import org.scoula.domain.fraud.dto.response.RiskAnalysisResponse;
import org.scoula.domain.fraud.dto.response.RiskCheckDetailResponse;
import org.scoula.domain.fraud.dto.response.RiskCheckListResponse;
import org.scoula.domain.fraud.dto.response.RiskCheckSummaryResponse;
import org.scoula.domain.fraud.enums.AnalysisStatus;
import org.scoula.domain.fraud.enums.RiskType;
import org.scoula.domain.fraud.exception.FraudErrorCode;
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FraudRiskServiceImpl implements FraudRiskService {

      private final FraudRiskMapper fraudRiskMapper;
      private final HomeLikeMapper homeLikeMapper;
      private final S3ServiceInterface s3Service;
      private final AiFraudAnalyzerService aiFraudAnalyzerService;

      // 허용된 파일 확장자
      private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("pdf", "PDF");
      private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

      // S3 파일 경로 패턴
      private static final String S3_BASE_PATH = "fraud";
      private static final String REGISTRY_FILE_PREFIX = "registry";
      private static final String BUILDING_FILE_PREFIX = "building";
      private static final String FILE_EXTENSION = ".pdf";

      @Override
      @Transactional
      public DocumentAnalysisResponse analyzeDocuments(
              Long userId, MultipartFile registryFile, MultipartFile buildingFile, Long homeId) {
          log.info("문서 분석 시작 - homeId: {}", homeId);

          // 1. 파일 유효성 검증
          validateFile(registryFile, "등기부등본");
          validateFile(buildingFile, "건축물대장");

          // 2. 매물 ID가 있는 경우에만 검증 (서비스 외 매물은 homeId가 없을 수 있음)
          if (homeId != null && homeId > 0) {
              // 매물 존재 여부 확인
              if (!fraudRiskMapper.existsHome(homeId)) {
                  throw new FraudRiskException(
                          FraudErrorCode.FRAUD_CHECK_NOT_FOUND, "존재하지 않는 매물입니다.");
              }
          }

          LocalDateTime startTime = LocalDateTime.now();

          try {
              // 3. S3에 파일 업로드
              String registryFileName = buildS3FilePath(userId, homeId, REGISTRY_FILE_PREFIX);
              String buildingFileName = buildS3FilePath(userId, homeId, BUILDING_FILE_PREFIX);

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
              throw new FraudRiskException(
                      FraudErrorCode.DOCUMENT_PROCESSING_FAILED,
                      "문서 분석 중 오류가 발생했습니다: " + e.getMessage());
          }
      }

      @Override
      @Transactional
      public RiskAnalysisResponse analyzeRisk(Long userId, RiskAnalysisRequest request) {
          log.info("위험도 분석 시작 - homeId: {}", request.getHomeId());

          // homeId 필수 체크
          if (request.getHomeId() == null || request.getHomeId() <= 0) {
              throw new FraudRiskException(FraudErrorCode.FRAUD_CHECK_NOT_FOUND, "유효한 매물 ID가 필요합니다.");
          }

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

                  log.debug(
                          "AI 분석 결과 - riskType: {}, riskScore: {}",
                          riskType,
                          aiResponse != null ? aiResponse.getRiskScore() : null);
              } catch (Exception e) {
                  log.error("AI 분석 실패", e);
                  throw new FraudRiskException(
                          FraudErrorCode.AI_SERVICE_UNAVAILABLE,
                          "AI 분석 중 오류가 발생했습니다: " + e.getMessage());
              }

              // 3. risk_check 업데이트
              riskCheck.setRiskType(riskType);
              fraudRiskMapper.updateRiskCheck(riskCheck);

              // 4. 기존 상세 분석 결과 삭제 (중복 방지)
              fraudRiskMapper.deleteRiskCheckDetail(riskCheck.getRiskckId());

              // 5. AI 분석 결과를 risk_check_detail에 저장
              saveAnalysisResultsToDb(aiResponse, riskCheck.getRiskckId());

              // 6. 저장된 상세 분석 결과 조회 및 그룹화
              List<RiskCheckDetailResponse.DetailGroup> detailGroups =
                      getDetailGroupsFromDb(riskCheck.getRiskckId());

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
              throw new FraudRiskException(
                      FraudErrorCode.RISK_CALCULATION_ERROR,
                      "위험도 분석 중 오류가 발생했습니다: " + e.getMessage());
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
              throw new FraudRiskException(FraudErrorCode.FRAUD_CHECK_ACCESS_DENIED);
          }

          RiskCheckDetailResponse response =
                  fraudRiskMapper.selectRiskCheckDetailResponse(riskCheckId);
          if (response == null) {
              throw new FraudRiskException(FraudErrorCode.FRAUD_CHECK_NOT_FOUND);
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
          List<RiskCheckDetailResponse.DetailGroup> detailGroups = getDetailGroupsFromDb(riskCheckId);
          response.setDetailGroups(detailGroups);

          return response;
      }

      @Override
      @Transactional
      public void deleteRiskCheck(Long userId, Long riskCheckId) {
          // 권한 확인
          if (!fraudRiskMapper.isOwnerOfRiskCheck(riskCheckId, userId)) {
              throw new FraudRiskException(
                      FraudErrorCode.FRAUD_CHECK_ACCESS_DENIED, "해당 위험도 체크 결과에 대한 삭제 권한이 없습니다.");
          }

          // 삭제하기 전에 파일 URL 조회
          RiskCheckDetailResponse riskCheck =
                  fraudRiskMapper.selectRiskCheckDetailResponse(riskCheckId);

          // 상세 정보 먼저 삭제
          fraudRiskMapper.deleteRiskCheckDetail(riskCheckId);

          // 메인 정보 삭제
          int deleted = fraudRiskMapper.deleteRiskCheck(riskCheckId);
          if (deleted == 0) {
              throw new FraudRiskException(
                      FraudErrorCode.FRAUD_ANALYSIS_FAILED, "위험도 체크 결과 삭제에 실패했습니다.");
          }

          // S3 파일 삭제 (실패해도 에러는 발생시키지 않음)
          if (riskCheck != null) {
              deleteS3Files(riskCheck.getRegistryFileUrl(), riskCheck.getBuildingFileUrl());
          }
      }

      // ========== Private Helper Methods ==========

      private void validateFile(MultipartFile file, String fileType) {
          if (file == null || file.isEmpty()) {
              throw new FraudRiskException(
                      FraudErrorCode.MISSING_REQUIRED_FIELDS, fileType + " 파일이 없습니다.");
          }

          if (file.getSize() > MAX_FILE_SIZE) {
              throw new FraudRiskException(
                      FraudErrorCode.INVALID_DOCUMENT_FORMAT, fileType + " 파일 크기가 10MB를 초과합니다.");
          }

          String filename = file.getOriginalFilename();
          if (filename == null || !hasValidExtension(filename)) {
              throw new FraudRiskException(
                      FraudErrorCode.UNSUPPORTED_DOCUMENT_TYPE, fileType + "은(는) PDF 파일만 업로드 가능합니다.");
          }
      }

      private boolean hasValidExtension(String filename) {
          if (filename == null || !filename.contains(".")) {
              return false;
          }
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

      @Override
      @Transactional
      public RiskAnalysisResponse analyzeExternalRisk(
              Long userId, ExternalRiskAnalysisRequest request) {
          log.info("서비스 외 매물 위험도 분석 시작 - userId: {}", userId);

          try {
              // AI 분석 서비스 호출
              FraudRiskCheckDto.Response aiResponse = null;
              RiskType riskType = RiskType.WARN; // 기본값을 WARN으로 설정

              try {
                  // AI 서버에 분석 요청 (ExternalRiskAnalysisRequest 직접 전달)
                  aiResponse = aiFraudAnalyzerService.analyzeFraudRisk(userId, request);
                  riskType = aiFraudAnalyzerService.determineRiskType(aiResponse);

                  log.debug(
                          "AI 분석 결과 - riskType: {}, riskScore: {}",
                          riskType,
                          aiResponse != null ? aiResponse.getRiskScore() : null);
              } catch (Exception e) {
                  log.error("AI 분석 실패", e);
                  throw new FraudRiskException(
                          FraudErrorCode.AI_SERVICE_UNAVAILABLE,
                          "AI 분석 중 오류가 발생했습니다: " + e.getMessage());
              }

              // AI 분석 결과를 DetailGroup으로 변환 (DB 저장 없이)
              List<RiskCheckDetailResponse.DetailGroup> detailGroups =
                      convertToDetailGroups(
                              aiResponse != null ? aiResponse.getAnalysisResults() : null,
                              aiResponse != null ? aiResponse.getRecommendations() : null);

              // 응답 반환 (riskCheckId는 null)
              return RiskAnalysisResponse.builder()
                      .riskCheckId(null)
                      .riskType(riskType)
                      .analyzedAt(LocalDateTime.now())
                      .detailGroups(detailGroups)
                      .build();

          } catch (FraudRiskException e) {
              // FraudRiskException은 그대로 다시 던지기 (에러 코드 보존)
              throw e;
          } catch (Exception e) {
              log.error("서비스 외 매물 위험도 분석 실패", e);
              throw new FraudRiskException(
                      FraudErrorCode.RISK_CALCULATION_ERROR,
                      "위험도 분석 중 오류가 발생했습니다: " + e.getMessage());
          }
      }

      /**
       * S3 파일 경로 생성 헬퍼 메소드
       *
       * @param userId 사용자 ID
       * @param homeId 매물 ID
       * @param filePrefix 파일 접두사 (registry 또는 building)
       * @return 생성된 S3 파일 경로
       */
      private String buildS3FilePath(Long userId, Long homeId, String filePrefix) {
          StringBuilder pathBuilder = new StringBuilder();
          pathBuilder.append(S3_BASE_PATH).append("/");
          pathBuilder.append(userId).append("/");
          pathBuilder.append(filePrefix).append("_");
          pathBuilder.append(homeId != null ? homeId : "quick").append("_");
          pathBuilder.append(System.currentTimeMillis());
          pathBuilder.append(FILE_EXTENSION);
          return pathBuilder.toString();
      }

      /**
       * S3에서 파일 삭제 헬퍼 메소드
       *
       * @param registryFileUrl 등기부등본 파일 URL
       * @param buildingFileUrl 건축물대장 파일 URL
       */
      private void deleteS3Files(String registryFileUrl, String buildingFileUrl) {
          // 등기부등본 파일 삭제
          if (registryFileUrl != null && !registryFileUrl.isEmpty()) {
              try {
                  String registryKey = extractS3KeyFromUrl(registryFileUrl);
                  if (registryKey != null) {
                      s3Service.deleteFile(registryKey);
                      log.debug("S3 파일 삭제 성공 - registryKey: {}", registryKey);
                  }
              } catch (Exception e) {
                  log.error("등기부등본 파일 삭제 실패 - URL: {}", registryFileUrl, e);
              }
          }

          // 건축물대장 파일 삭제
          if (buildingFileUrl != null && !buildingFileUrl.isEmpty()) {
              try {
                  String buildingKey = extractS3KeyFromUrl(buildingFileUrl);
                  if (buildingKey != null) {
                      s3Service.deleteFile(buildingKey);
                      log.debug("S3 파일 삭제 성공 - buildingKey: {}", buildingKey);
                  }
              } catch (Exception e) {
                  log.error("건축물대장 파일 삭제 실패 - URL: {}", buildingFileUrl, e);
              }
          }
      }

      /**
       * S3 URL에서 키 추출 헬퍼 메소드
       *
       * @param url S3 파일 URL
       * @return S3 키
       */
      private String extractS3KeyFromUrl(String url) {
          if (url == null || url.isEmpty()) {
              return null;
          }

          // URL에서 S3 키 부분만 추출
          // 예: https://bucket-name.s3.region.amazonaws.com/fraud/123/registry_456_1234567890.pdf
          // → fraud/123/registry_456_1234567890.pdf

          try {
              // S3_BASE_PATH로 시작하는 부분을 찾아서 추출
              int index = url.indexOf(S3_BASE_PATH);
              if (index != -1) {
                  return url.substring(index);
              }

              // 또는 마지막 '/' 이후의 전체 경로를 추출하는 방법
              // amazonaws.com/ 이후의 경로를 추출
              String marker = ".amazonaws.com/";
              index = url.indexOf(marker);
              if (index != -1) {
                  return url.substring(index + marker.length());
              }
          } catch (Exception e) {
              log.error("S3 키 추출 실패 - URL: {}", url, e);
          }

          return null;
      }

      /**
       * AI 분석 결과를 DetailGroup 리스트로 변환
       *
       * @param analysisResults AI 분석 결과
       * @param recommendations 추천사항 리스트
       * @return DetailGroup 리스트
       */
      private List<RiskCheckDetailResponse.DetailGroup> convertToDetailGroups(
              Map<String, Object> analysisResults, List<String> recommendations) {
          List<RiskCheckDetailResponse.DetailGroup> detailGroups = new ArrayList<>();

          // 분석 결과 변환
          if (analysisResults != null) {
              detailGroups.addAll(convertAnalysisResultsToGroups(analysisResults));
          }

          // 추천사항 추가
          if (recommendations != null && !recommendations.isEmpty()) {
              detailGroups.add(createRecommendationGroup(recommendations));
          }

          return detailGroups;
      }

      /** 분석 결과를 DetailGroup으로 변환 */
      private List<RiskCheckDetailResponse.DetailGroup> convertAnalysisResultsToGroups(
              Map<String, Object> analysisResults) {
          List<RiskCheckDetailResponse.DetailGroup> groups = new ArrayList<>();

          for (Map.Entry<String, Object> groupEntry : analysisResults.entrySet()) {
              String title1 = groupEntry.getKey();
              Object groupValue = groupEntry.getValue();

              if (groupValue instanceof Map) {
                  @SuppressWarnings("unchecked")
                  Map<String, Object> groupItems = (Map<String, Object>) groupValue;

                  List<RiskCheckDetailResponse.DetailItem> items = createDetailItems(groupItems);
                  if (!items.isEmpty()) {
                      groups.add(
                              RiskCheckDetailResponse.DetailGroup.builder()
                                      .title(title1)
                                      .items(items)
                                      .build());
                  }
              }
          }
          return groups;
      }

      /** 상세 항목 생성 */
      private List<RiskCheckDetailResponse.DetailItem> createDetailItems(
              Map<String, Object> groupItems) {
          List<RiskCheckDetailResponse.DetailItem> items = new ArrayList<>();

          for (Map.Entry<String, Object> itemEntry : groupItems.entrySet()) {
              Object itemValue = itemEntry.getValue();

              if (itemValue instanceof Map) {
                  @SuppressWarnings("unchecked")
                  Map<String, Object> itemDetails = (Map<String, Object>) itemValue;

                  String title2 = itemDetails.getOrDefault("title", "").toString();
                  String content = itemDetails.getOrDefault("content", "").toString();

                  items.add(
                          RiskCheckDetailResponse.DetailItem.builder()
                                  .title(title2)
                                  .content(content)
                                  .build());
              }
          }
          return items;
      }

      /** 추천사항 그룹 생성 */
      private RiskCheckDetailResponse.DetailGroup createRecommendationGroup(
              List<String> recommendations) {
          String recommendationsContent = String.join("\n", recommendations);
          RiskCheckDetailResponse.DetailItem recommendItem =
                  RiskCheckDetailResponse.DetailItem.builder()
                          .title("AI 분석 기반 추천")
                          .content(recommendationsContent)
                          .build();

          return RiskCheckDetailResponse.DetailGroup.builder()
                  .title("추천사항")
                  .items(Collections.singletonList(recommendItem))
                  .build();
      }

      /**
       * AI 분석 결과를 DB에 저장
       *
       * @param aiResponse AI 응답
       * @param riskCheckId risk_check ID
       */
      private void saveAnalysisResultsToDb(FraudRiskCheckDto.Response aiResponse, Long riskCheckId) {
          if (aiResponse == null || riskCheckId == null) {
              return;
          }

          // 분석 결과 저장
          if (aiResponse.getAnalysisResults() != null) {
              saveAnalysisResults(aiResponse.getAnalysisResults(), riskCheckId);
          }

          // 추천사항 저장
          if (aiResponse.getRecommendations() != null && !aiResponse.getRecommendations().isEmpty()) {
              saveRecommendations(aiResponse.getRecommendations(), riskCheckId);
          }
      }

      /** 분석 결과를 DB에 저장 */
      private void saveAnalysisResults(Map<String, Object> analysisResults, Long riskCheckId) {
          for (Map.Entry<String, Object> groupEntry : analysisResults.entrySet()) {
              String title1 = groupEntry.getKey();
              Object groupValue = groupEntry.getValue();

              if (groupValue instanceof Map) {
                  @SuppressWarnings("unchecked")
                  Map<String, Object> groupItems = (Map<String, Object>) groupValue;
                  saveGroupItems(title1, groupItems, riskCheckId);
              }
          }
      }

      /** 그룹 항목들을 DB에 저장 */
      private void saveGroupItems(String title1, Map<String, Object> groupItems, Long riskCheckId) {
          for (Map.Entry<String, Object> itemEntry : groupItems.entrySet()) {
              Object itemValue = itemEntry.getValue();

              if (itemValue instanceof Map) {
                  @SuppressWarnings("unchecked")
                  Map<String, Object> itemDetails = (Map<String, Object>) itemValue;

                  String title2 = itemDetails.getOrDefault("title", "").toString();
                  String content = itemDetails.getOrDefault("content", "").toString();

                  saveRiskCheckDetail(riskCheckId, title1, title2, content);
              }
          }
      }

      /** 추천사항을 DB에 저장 */
      private void saveRecommendations(List<String> recommendations, Long riskCheckId) {
          String content = String.join("\n", recommendations);
          saveRiskCheckDetail(riskCheckId, "추천사항", "AI 분석 기반 추천", content);
      }

      /** 위험도 체크 상세 정보를 DB에 저장 */
      private void saveRiskCheckDetail(
              Long riskCheckId, String title1, String title2, String content) {
          RiskCheckDetailVO detail =
                  RiskCheckDetailVO.builder()
                          .riskckId(riskCheckId)
                          .title1(title1)
                          .title2(title2)
                          .content(content)
                          .build();

          try {
              fraudRiskMapper.insertRiskCheckDetail(detail);
              log.debug("상세 분석 결과 저장 성공 - title1: {}, title2: {}", title1, title2);
          } catch (Exception e) {
              log.warn(
                      "상세 분석 결과 저장 실패 - title1: {}, title2: {}, error: {}",
                      title1,
                      title2,
                      e.getMessage());
          }
      }

      /**
       * DB에서 상세 분석 결과를 조회하여 DetailGroup으로 변환
       *
       * @param riskCheckId risk_check ID
       * @return DetailGroup 리스트
       */
      private List<RiskCheckDetailResponse.DetailGroup> getDetailGroupsFromDb(Long riskCheckId) {
          List<RiskCheckDetailVO> details =
                  fraudRiskMapper.selectRiskCheckDetailByRiskCheckId(riskCheckId);

          if (details == null || details.isEmpty()) {
              return new ArrayList<>();
          }

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
                                                                      .content(detail.getContent())
                                                                      .build())
                                              .collect(Collectors.toList()))
                              .build();
              detailGroups.add(group);
          }

          return detailGroups;
      }

      @Override
      public RiskCheckSummaryResponse getTodayRiskCheckSummary(Long userId, Long homeId) {
          log.info("오늘 분석한 위험도 체크 요약 조회 - userId: {}, homeId: {}", userId, homeId);

          LocalDateTime[] todayRange = getTodayDateRange();

          // 오늘 분석한 위험도 체크 ID 조회
          Long riskCheckId =
                  fraudRiskMapper.selectTodayRiskCheckId(
                          userId, homeId, todayRange[0], todayRange[1]);

          if (riskCheckId == null) {
              return null; // 오늘 분석한 결과가 없는 경우
          }

          // 위험도 체크 정보 조회
          RiskCheckVO riskCheck = fraudRiskMapper.selectRiskCheckById(riskCheckId);
          if (riskCheck == null) {
              return null;
          }

          // 상세 분석 결과 조회 및 변환
          List<RiskCheckSummaryResponse.DetailGroup> detailGroups =
                  convertToSummaryDetailGroups(riskCheckId);

          return RiskCheckSummaryResponse.builder()
                  .riskCheckId(riskCheck.getRiskckId())
                  .riskType(riskCheck.getRiskType().name())
                  .detailGroups(detailGroups)
                  .build();
      }

      /** 오늘의 시작과 끝 시간 반환 */
      private LocalDateTime[] getTodayDateRange() {
          LocalDate today = LocalDate.now();
          return new LocalDateTime[] {today.atStartOfDay(), today.atTime(23, 59, 59)};
      }

      /** DB에서 조회한 상세 정보를 RiskCheckSummaryResponse.DetailGroup으로 변환 */
      private List<RiskCheckSummaryResponse.DetailGroup> convertToSummaryDetailGroups(
              Long riskCheckId) {
          List<RiskCheckDetailVO> details =
                  fraudRiskMapper.selectRiskCheckDetailByRiskCheckId(riskCheckId);

          if (details == null || details.isEmpty()) {
              return new ArrayList<>();
          }

          // title1별로 그룹화
          Map<String, List<RiskCheckDetailVO>> groupedDetails =
                  details.stream()
                          .collect(
                                  Collectors.groupingBy(
                                          RiskCheckDetailVO::getTitle1,
                                          LinkedHashMap::new,
                                          Collectors.toList()));

          // DetailGroup으로 변환
          return groupedDetails.entrySet().stream()
                  .map(
                          entry -> {
                              List<RiskCheckSummaryResponse.DetailItem> items =
                                      entry.getValue().stream()
                                              .map(
                                                      detail ->
                                                              RiskCheckSummaryResponse.DetailItem
                                                                      .builder()
                                                                      .title(detail.getTitle2())
                                                                      .content(detail.getContent())
                                                                      .build())
                                              .collect(Collectors.toList());

                              return RiskCheckSummaryResponse.DetailGroup.builder()
                                      .title(entry.getKey())
                                      .items(items)
                                      .build();
                          })
                  .collect(Collectors.toList());
      }
}
