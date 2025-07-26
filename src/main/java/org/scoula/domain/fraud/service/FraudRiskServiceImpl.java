package org.scoula.domain.fraud.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

          // 2. 매물 존재 여부 확인
          if (!fraudRiskMapper.existsHome(homeId)) {
              throw new FraudRiskException("존재하지 않는 매물입니다. homeId: " + homeId);
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

              // TODO: 실제 OCR 서비스 연동 구현 필요
              // 임시 OCR 데이터 생성
              RegistryDocumentDto registryDoc =
                      RegistryDocumentDto.builder()
                              .regionAddress("서울특별시 강남구 역삼동 123-45번지")
                              .roadAddress("서울특별시 강남구 테헤란로 123")
                              .ownerName("홍길동")
                              .ownerBirthDate(LocalDate.of(1980, 1, 15))
                              .maxClaimAmount(500000000L)
                              .debtor("홍길동")
                              .mortgagee("XX은행")
                              .hasSeizure(false)
                              .hasAuction(false)
                              .hasLitigation(false)
                              .hasAttachment(false)
                              .build();

              BuildingDocumentDto buildingDoc =
                      BuildingDocumentDto.builder()
                              .siteLocation("서울특별시 강남구 역삼동 123-45번지")
                              .roadAddress("서울특별시 강남구 테헤란로 123")
                              .totalFloorArea(84.5)
                              .purpose("아파트")
                              .floorNumber(15)
                              .approvalDate(LocalDate.of(2020, 3, 20))
                              .isViolationBuilding(false)
                              .build();

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

          } catch (Exception e) {
              log.error("문서 분석 중 오류 발생", e);

              return DocumentAnalysisResponse.builder()
                      .homeId(homeId)
                      .registryAnalysisStatus(AnalysisStatus.FAILED.name())
                      .buildingAnalysisStatus(AnalysisStatus.FAILED.name())
                      .registryAnalyzedAt(startTime)
                      .buildingAnalyzedAt(LocalDateTime.now())
                      .processingTime(0.0)
                      .errorMessage(e.getMessage())
                      .build();
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

              // 2. AI 분석 (실제 구현 시 외부 AI 서비스 호출)
              // TODO: 실제 AI 분석 서비스 연동 구현 필요
              RiskType riskType = RiskType.SAFE; // 임시로 SAFE 설정

              // 3. risk_check 업데이트
              riskCheck.setRiskType(riskType);
              fraudRiskMapper.updateRiskCheck(riskCheck);

              // 4. 기존 상세 분석 결과 삭제 (중복 방지)
              fraudRiskMapper.deleteRiskCheckDetail(riskCheck.getRiskckId());

              // 5. 상세 분석 결과 저장 (여러 개의 detail 저장)
              // title1별로 하나의 레코드만 저장하도록 수정
              // 5-1. 기본 정보 분석 결과 (하나의 레코드로 통합)
              RiskCheckDetailVO basicDetail =
                      RiskCheckDetailVO.builder()
                              .riskckId(riskCheck.getRiskckId())
                              .title1("갑기본정보")
                              .title2("소유 및 주소")
                              .content("등기부등본의 소유자가 임대인 정보와 일치하며, 주소가 정확히 일치합니다.")
                              .build();
              fraudRiskMapper.insertRiskCheckDetail(basicDetail);

              // 5-2. 권리 관계 분석 결과
              RiskCheckDetailVO rightsDetail =
                      RiskCheckDetailVO.builder()
                              .riskckId(riskCheck.getRiskckId())
                              .title1("을기사항")
                              .title2("근저당권")
                              .content(
                                      riskType == RiskType.SAFE
                                              ? "설정된 근저당권이 없거나 적정 수준입니다."
                                              : "근저당 설정액이 매매가 대비 과도합니다. 확인이 필요합니다.")
                              .build();
              fraudRiskMapper.insertRiskCheckDetail(rightsDetail);

              // 5-3. 건물 정보 분석 결과 (하나의 레코드로 통합)
              RiskCheckDetailVO buildingDetail =
                      RiskCheckDetailVO.builder()
                              .riskckId(riskCheck.getRiskckId())
                              .title1("건축물대장")
                              .title2("건물 상태")
                              .content(
                                      riskType != RiskType.DANGER
                                              ? "위반건축물이 아닌 적법한 건축물이며, 주거용으로 적합하게 등록되어 있습니다."
                                              : "위반건축물로 확인되며 주의가 필요합니다.")
                              .build();
              fraudRiskMapper.insertRiskCheckDetail(buildingDetail);

              // 5-4. 종합 평가 (하나의 레코드로 통합)
              String recommendationContent =
                      riskType == RiskType.SAFE
                              ? "안전한 거래가 가능합니다. 계약 진행 시 표준 계약서를 사용하세요."
                              : riskType == RiskType.WARN
                                      ? "추가 확인이 필요합니다. 전문가 상담을 권장합니다."
                                      : "거래를 재검토하세요. 법률 전문가의 도움을 받으시기 바랍니다.";

              RiskCheckDetailVO summaryDetail =
                      RiskCheckDetailVO.builder()
                              .riskckId(riskCheck.getRiskckId())
                              .title1("종합평가")
                              .title2(getTitleByRiskType(riskType))
                              .content(generateSummary(riskType) + " " + recommendationContent)
                              .build();
              fraudRiskMapper.insertRiskCheckDetail(summaryDetail);

              // 5-5. 추가 분석 항목 제거 (title1당 하나의 레코드만 유지)

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

          } catch (Exception e) {
              log.error("위험도 분석 실패", e);
              throw new FraudRiskException("위험도 분석 중 오류가 발생했습니다: " + e.getMessage());
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
              throw new FraudRiskException(fileType + " 파일이 없습니다.");
          }

          if (file.getSize() > MAX_FILE_SIZE) {
              throw new FraudRiskException(fileType + " 파일 크기가 10MB를 초과합니다.");
          }

          String filename = file.getOriginalFilename();
          if (filename == null || !hasValidExtension(filename)) {
              throw new FraudRiskException(fileType + "은(는) PDF 파일만 업로드 가능합니다.");
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

      private String generateSummary(RiskType riskType) {
          switch (riskType) {
              case SAFE:
                  return "분석 결과 안전한 매물로 판단됩니다. 등기부등본과 건축물대장의 정보가 일치하며, 권리관계가 깨끗합니다.";
              case WARN:
                  return "주의가 필요한 사항이 발견되었습니다. 일부 확인이 필요한 부분이 있으니 전문가와 상담을 권장합니다.";
              case DANGER:
                  return "위험 요소가 발견되었습니다. 근저당 설정이 과도하거나 권리관계에 문제가 있을 수 있습니다.";
              default:
                  return "분석이 완료되었습니다.";
          }
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
