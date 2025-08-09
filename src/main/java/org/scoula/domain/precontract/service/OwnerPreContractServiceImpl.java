package org.scoula.domain.precontract.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.scoula.domain.chat.document.SpecialContractDocument;
import org.scoula.domain.precontract.document.ContractDocumentMongoDocument;
import org.scoula.domain.precontract.document.OwnerMongoDocument;
import org.scoula.domain.precontract.dto.ai.ClauseRecommendRequestDto;
import org.scoula.domain.precontract.dto.ai.ClauseRecommendResponseDto;
import org.scoula.domain.precontract.dto.ai.ContractParseResponseDto;
import org.scoula.domain.precontract.dto.common.IdentityVerificationInfoDTO;
import org.scoula.domain.precontract.dto.owner.*;
import org.scoula.domain.precontract.enums.RentType;
import org.scoula.domain.precontract.exception.OwnerPreContractErrorCode;
import org.scoula.domain.precontract.mapper.OwnerPreContractMapper;
import org.scoula.domain.precontract.repository.ContractDocumentMongoRepository;
import org.scoula.domain.precontract.repository.OwnerMongoRepository;
import org.scoula.domain.precontract.vo.IdentityVerificationInfoVO;
import org.scoula.domain.precontract.vo.OwnerJeonseInfoVO;
import org.scoula.domain.precontract.vo.OwnerWolseInfoVO;
import org.scoula.domain.precontract.vo.RestoreCategoryVO;
import org.scoula.domain.verification.dto.request.IdCardVerificationRequest;
import org.scoula.domain.verification.service.IdCardVerificationService;
import org.scoula.global.common.exception.BusinessException;
import org.scoula.global.common.util.AesCryptoUtil;
import org.scoula.global.common.util.LogSanitizerUtil;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class OwnerPreContractServiceImpl implements OwnerPreContractService {
      private final IdCardVerificationService idCardVerificationService;
      private final OwnerPreContractMapper ownerMapper;
      private final OwnerMongoRepository mongoRepository;
      private final ContractDocumentMongoRepository contractDocumentMongoRepository;
      private final AiContractAnalyzerService aiContractAnalyzerService;
      private final AiClauseRecommendService aiClauseRecommendService;
      private final MongoTemplate mongoTemplate;
      private final ObjectMapper objectMapper;
      private final AesCryptoUtil aesCryptoUtil;

      @Override
      public Void requireVerification(
              Long contractChatId, Long userId, IdentityVerificationInfoDTO dto) {
          // 1. 진위 확인 요청 DTO 구성 (※ 실명 인증용이므로 평문 사용)
          IdCardVerificationRequest request =
                  IdCardVerificationRequest.builder()
                          .name(dto.getName())
                          .rrn1(dto.getSsnFront())
                          .rrn2(dto.getSsnBack())
                          .date(dto.getIssuedDate())
                          .build();

          // 2. 신분증 진위 확인
          boolean result = idCardVerificationService.verifyIdCard(request);

          if (!result) {
              log.warn("신분증 진위 확인 실패");
              throw new RuntimeException("신분증 검증 실패");
          }

          log.info("신분증 진위 확인 성공");

          // 3. 민감 정보 암호화 처리
          String encryptedSsnBack = aesCryptoUtil.encrypt(dto.getSsnBack());
          String encryptedPhoneNumber = aesCryptoUtil.encrypt(dto.getPhoneNumber());

          // 주소는 선택적 암호화 (민감도에 따라 결정)
          String encryptedAddr2 =
                  dto.getAddr2() != null ? aesCryptoUtil.encrypt(dto.getAddr2()) : null;

          IdentityVerificationInfoVO vo =
                  IdentityVerificationInfoVO.builder()
                          .userId(userId)
                          .name(dto.getName()) // 이름은 평문 저장 (계약서 표시용)
                          .ssnFront(dto.getSsnFront()) // 앞자리는 평문 저장
                          .ssnBack(encryptedSsnBack) // 뒷자리는 암호화
                          .addr1(dto.getAddr1()) // 기본 주소는 평문
                          .addr2(encryptedAddr2) // 상세 주소는 암호화
                          .phoneNumber(encryptedPhoneNumber) // 전화번호 암호화
                          .build();

          // 4. 기존 데이터 확인 후 저장 또는 업데이트
          Optional<IdentityVerificationInfoVO> existingInfo =
                  ownerMapper.selectIdentityVerificationInfo(contractChatId, userId);

          if (existingInfo.isPresent()) {
              // 기존 데이터가 있으면 업데이트
              int updated = ownerMapper.updateIdentityVerification(contractChatId, vo);
              if (updated == 0) {
                  throw new BusinessException(
                          OwnerPreContractErrorCode.OWNER_UPDATE, "본인 인증 정보 업데이트에 실패했습니다.");
              }
              log.info("본인 인증 정보 업데이트 완료 - contractChatId: {}, userId: {}", contractChatId, userId);
          } else {
              // 기존 데이터가 없으면 새로 저장
              ownerMapper.insertIdentityVerification(contractChatId, userId, vo);
              log.info("본인 인증 정보 신규 저장 완료 - contractChatId: {}, userId: {}", contractChatId, userId);
          }

          return null;
      }

      @Override
      @Transactional
      public OwnerInitRespDTO saveOwnerInfo(Long contractChatId, Long ownerId) {
          validateUser(contractChatId, ownerId, false);

          Long identityId =
                  ownerMapper
                          .selectIdentityId(contractChatId)
                          .orElseThrow(
                                  () ->
                                          new BusinessException(
                                                  OwnerPreContractErrorCode.OWNER_SELECT));

          String rentType =
                  ownerMapper
                          .selectRentType(contractChatId, ownerId)
                          .orElseThrow(
                                  () ->
                                          new BusinessException(
                                                  OwnerPreContractErrorCode.OWNER_SELECT));

          int inserted = ownerMapper.insertOwnerPreContractSet(contractChatId, identityId, rentType);
          if (inserted == 0) {
              throw new BusinessException(OwnerPreContractErrorCode.OWNER_INSERT);
          }

          if ("JEONSE".equalsIgnoreCase(rentType)) {
              OwnerJeonseInfoVO jeonseInfo =
                      OwnerJeonseInfoVO.builder().contractChatId(contractChatId).build();
              ownerMapper.insertJeonseInfo(jeonseInfo);
          } else if ("WOLSE".equalsIgnoreCase(rentType)) {
              OwnerWolseInfoVO wolseInfo =
                      OwnerWolseInfoVO.builder().contractChatId(contractChatId).build();
              ownerMapper.insertWolseInfo(wolseInfo);
          }

          return OwnerInitRespDTO.toResp(rentType);
      }

      @Override
      public Void updateOwnerContractStep1(
              Long contractChatId, Long userId, OwnerContractStep1DTO contractStep1DTO) {
          validateUser(contractChatId, userId, true);

          int result = ownerMapper.updateContractSub1(contractChatId, userId, contractStep1DTO);
          if (result != 1) throw new BusinessException(OwnerPreContractErrorCode.OWNER_UPDATE);
          return null;
      }

      @Override
      public OwnerContractStep1DTO selectOwnerContractStep1(Long contractChatId, Long userId) {
          validateUser(contractChatId, userId, true);
          return ownerMapper
                  .selectContractSub1(contractChatId, userId)
                  .orElseThrow(() -> new BusinessException(OwnerPreContractErrorCode.OWNER_SELECT));
      }

      @Override
      public Void updateOwnerContractStep2(
              Long contractChatId, Long userId, OwnerContractStep2DTO dto) {

          validateUser(contractChatId, userId, true);

          // 1. 계약 조건 업데이트
          ownerMapper.updateContractSub2(dto, contractChatId, userId);

          // 2. ownerPrecheckId 조회
          Long ownerPrecheckId =
                  ownerMapper
                          .selectOwnerPrecheckId(contractChatId, userId)
                          .orElseThrow(
                                  () ->
                                          new BusinessException(
                                                  OwnerPreContractErrorCode.OWNER_SELECT));

          // 3. 복구 범위 upsert - restoreCategories(name) 사용
          if (dto.getRestoreCategories() != null && !dto.getRestoreCategories().isEmpty()) {
              for (String categoryName : dto.getRestoreCategories()) {
                  Long categoryId = ownerMapper.selectRestoreCategoryIdByName(categoryName);
                  if (categoryId == null) {
                      throw new BusinessException(OwnerPreContractErrorCode.ENUM_VALUE_OF);
                  }
                  ownerMapper.upsertRestoreScope(ownerPrecheckId, categoryId);
              }
          }

          return null;
      }

      @Override
      public OwnerContractStep2DTO selectOwnerContractStep2(Long contractChatId, Long userId) {
          validateUser(contractChatId, userId, true);

          OwnerContractStep2DTO dto =
                  ownerMapper
                          .selectContractSub2(contractChatId, userId)
                          .orElseThrow(
                                  () ->
                                          new BusinessException(
                                                  OwnerPreContractErrorCode.OWNER_SELECT));

          List<RestoreCategoryVO> categories = ownerMapper.selectRestoreScope(contractChatId, userId);
          List<String> categoryNames =
                  categories.stream()
                          .map(RestoreCategoryVO::getRestoreCategoryName)
                          .collect(Collectors.toList());
          dto.setRestoreCategories(categoryNames);

          return dto;
      }

      @Override
      public Void updateOwnerLivingStep1(Long contractChatId, Long userId, OwnerLivingStep1DTO dto) {
          validateUser(contractChatId, userId, true);

          // Step1 공통 정보 업데이트
          int updated = ownerMapper.updateLivingSub1(dto, contractChatId, userId);
          if (updated != 1) throw new BusinessException(OwnerPreContractErrorCode.OWNER_UPDATE);

          // rentType을 DB에서 직접 조회
          String rentTypeStr =
                  ownerMapper
                          .selectRentType(contractChatId, userId)
                          .orElseThrow(
                                  () ->
                                          new BusinessException(
                                                  OwnerPreContractErrorCode.OWNER_SELECT));
          RentType rentType = RentType.valueOf(rentTypeStr);

          // 월세일 경우 추가 정보 업데이트
          if (rentType == RentType.WOLSE) {
              Integer dueDate = dto.getPaymentDueDate();
              Double lateFee = dto.getLateFeeInterestRate();
              if (dueDate == null || lateFee == null)
                  throw new BusinessException(OwnerPreContractErrorCode.OWNER_MISSING_DATA);

              int wolseResult =
                      ownerMapper.updateLivingWolse(contractChatId, userId, dueDate, lateFee);
              if (wolseResult != 1)
                  throw new BusinessException(OwnerPreContractErrorCode.OWNER_UPDATE);
          }

          return null;
      }

      @Override
      public OwnerLivingStep1DTO selectOwnerLivingStep1(Long contractChatId, Long userId) {
          validateUser(contractChatId, userId, true);

          OwnerLivingStep1DTO dto =
                  ownerMapper
                          .selectLivingSub1(contractChatId, userId)
                          .orElseThrow(
                                  () ->
                                          new BusinessException(
                                                  OwnerPreContractErrorCode.OWNER_SELECT));

          // rentType을 DB에서 조회
          String rentTypeStr =
                  ownerMapper
                          .selectRentType(contractChatId, userId)
                          .orElseThrow(
                                  () ->
                                          new BusinessException(
                                                  OwnerPreContractErrorCode.RENT_TYPE_MISSING));

          RentType rentType = RentType.valueOf(rentTypeStr);

          if (rentType == RentType.WOLSE) {
              OwnerWolseInfoVO vo =
                      ownerMapper
                              .selectLivingWolse(contractChatId, userId)
                              .orElseThrow(
                                      () ->
                                              new BusinessException(
                                                      OwnerPreContractErrorCode.OWNER_SELECT));
              dto.setPaymentDueDate(vo.getPaymentDueDate());
              dto.setLateFeeInterestRate(vo.getLateFeeInterestRate());
          }

          return dto;
      }

      @Override
      public ContractParseResponseDto analyzeContractDocument(MultipartFile file) {
          try {
              log.info(
                      "계약서 특약 OCR 요청 시작 - 파일명: {}",
                      LogSanitizerUtil.sanitize(file.getOriginalFilename()));
              ContractParseResponseDto aiResponse =
                      aiContractAnalyzerService.parseContractDocument(file);

              log.info(
                      "계약서 특약 OCR 완료 - 특약 수: {}",
                      aiResponse != null
                                      && aiResponse.getData() != null
                                      && aiResponse.getData().getParsedData() != null
                                      && aiResponse.getData().getParsedData().getSpecialTerms()
                                              != null
                              ? aiResponse.getData().getParsedData().getSpecialTerms().size()
                              : 0);

              return aiResponse;
          } catch (BusinessException e) {
              log.error("AI 서비스 오류", e);
              throw e;
          } catch (Exception e) {
              log.error("특약 문서 분석 중 예상치 못한 오류", e);
              throw new BusinessException(OwnerPreContractErrorCode.OWNER_INSERT, e);
          }
      }

      @Override
      public void saveContractDocument(Long contractChatId, Long userId, ContractDocumentDTO dto) {
          try {
              ContractDocumentMongoDocument document =
                      ContractDocumentMongoDocument.from(contractChatId, userId, dto);
              ContractDocumentMongoDocument result = contractDocumentMongoRepository.save(document);
              log.info("특약 문서 Mongo 저장 완료: {}", result);
          } catch (DataAccessException e) {
              log.error("Mongo 저장 실패", e);
              throw new BusinessException(OwnerPreContractErrorCode.OWNER_INSERT, e);
          } catch (Exception e) {
              log.error("특약 문서 저장 중 예상치 못한 오류", e);
              throw new BusinessException(OwnerPreContractErrorCode.OWNER_INSERT, e);
          }
      }

      @Override
      public ContractDocumentMongoDocument getContractDocument(Long contractChatId, Long userId) {
          ContractDocumentMongoDocument document =
                  contractDocumentMongoRepository.findByContractChatIdAndUserId(
                          contractChatId, userId);
          if (document == null) {
              throw new BusinessException(OwnerPreContractErrorCode.OWNER_SELECT);
          }
          return document;
      }

      @Override
      public OwnerPreContractDTO selectOwnerPreContract(Long contractChatId, Long userId) {
          validateUser(contractChatId, userId, true);
          return ownerMapper
                  .selectOwnerPreContractSummary(contractChatId, userId)
                  .orElseThrow(() -> new BusinessException(OwnerPreContractErrorCode.OWNER_SELECT));
      }

      private void validateUser(Long contractChatId, Long userId, boolean requirePrecheck) {
          Optional<Long> contractOwnerIdOpt =
                  requirePrecheck
                          ? ownerMapper.selectContractOwnerId(contractChatId)
                          : ownerMapper.selectOwnerIdFromContractChat(contractChatId);

          Long contractOwnerId =
                  contractOwnerIdOpt.orElseThrow(
                          () -> new BusinessException(OwnerPreContractErrorCode.OWNER_USER));

          if (!contractOwnerId.equals(userId)) {
              throw new BusinessException(OwnerPreContractErrorCode.OWNER_USER);
          }
      }

      @Override
      @Transactional
      public Void saveMongoDB(Long contractChatId, Long userId) {
          validateOwnerForMongoDB(contractChatId, userId);

          OwnerPreContractMongoDTO dto = fetchOwnerData(contractChatId, userId);
          populateDTOSteps(dto);

          saveOwnerDocument(dto);
          processAiClauseRecommendation(contractChatId, userId, dto);

          return null;
      }

      private void validateOwnerForMongoDB(Long contractChatId, Long userId) {
          ownerMapper
                  .selectContractOwnerId(contractChatId)
                  .filter(ownerId -> ownerId.equals(userId))
                  .orElseThrow(() -> new BusinessException(OwnerPreContractErrorCode.OWNER_USER));
      }

      private OwnerPreContractMongoDTO fetchOwnerData(Long contractChatId, Long userId) {
          OwnerPreContractMongoDTO dto = ownerMapper.selectMongo(contractChatId, userId);
          if (dto == null) {
              throw new BusinessException(OwnerPreContractErrorCode.OWNER_SELECT);
          }
          return dto;
      }

      private void populateDTOSteps(OwnerPreContractMongoDTO dto) {
          dto.setContractStep1(
                  OwnerContractStep1DTO.builder()
                          .mortgaged(dto.getMortgaged())
                          .contractDuration(dto.getContractDuration())
                          .renewalIntent(dto.getRenewalIntent())
                          .responseRepairingFixtures(dto.getResponseRepairingFixtures())
                          .build());

          dto.setContractStep2(
                  OwnerContractStep2DTO.builder()
                          .hasConditionLog(dto.getHasConditionLog())
                          .hasPenalty(dto.getHasPenalty())
                          .hasPriorityForExtension(dto.getHasPriorityForExtension())
                          .hasAutoPriceAdjustment(dto.getHasAutoPriceAdjustment())
                          .allowJeonseRightRegistration(dto.getAllowJeonseRightRegistration())
                          .build());

          dto.setLivingStep1(
                  OwnerLivingStep1DTO.builder()
                          .requireRentGuaranteeInsurance(dto.getRequireRentGuaranteeInsurance())
                          .insuranceBurden(dto.getInsuranceBurden())
                          .hasNotice(dto.getHasNotice())
                          .ownerBankName(dto.getOwnerBankName())
                          .ownerAccountNumber(dto.getOwnerAccountNumber())
                          .paymentDueDate(dto.getPaymentDueDate())
                          .lateFeeInterestRate(dto.getLateFeeInterestRate())
                          .build());
      }

      private void saveOwnerDocument(OwnerPreContractMongoDTO dto) {
          try {
              OwnerMongoDocument document = OwnerMongoDocument.from(dto);
              log.info("변환된 document: {}", document);

              OwnerMongoDocument result = mongoRepository.insert(document);
              log.info("Mongo 저장 결과: {}", result);
          } catch (DataAccessException e) {
              log.error("Mongo 저장 실패", e);
              throw new BusinessException(OwnerPreContractErrorCode.OWNER_INSERT, e);
          }
      }

      private void processAiClauseRecommendation(
              Long contractChatId, Long userId, OwnerPreContractMongoDTO ownerDto) {
          try {
              ContractDocumentMongoDocument contractDocument =
                      contractDocumentMongoRepository.findByContractChatIdAndUserId(
                              contractChatId, userId);

              ClauseRecommendRequestDto aiRequest =
                      prepareAiRequest(contractChatId, userId, ownerDto, contractDocument);

              log.info("AI 특약 추천 요청 시작 - contractChatId: {}", contractChatId);
              logAiRequest(aiRequest);

              ClauseRecommendResponseDto aiResponse =
                      aiClauseRecommendService.recommendClauses(aiRequest);
              saveAiRecommendation(contractChatId, aiResponse);

          } catch (Exception e) {
              log.error("AI 특약 추천 중 오류 발생", e);
              throw new BusinessException(OwnerPreContractErrorCode.OWNER_INSERT, "AI 특약 추천 실패");
          }
      }

      private ClauseRecommendRequestDto prepareAiRequest(
              Long contractChatId,
              Long userId,
              OwnerPreContractMongoDTO ownerDto,
              ContractDocumentMongoDocument contractDocument) {

          Long identityId = ownerMapper.selectIdentityId(contractChatId).orElse(null);
          Long ownerPrecheckId =
                  ownerMapper.selectOwnerPrecheckId(contractChatId, userId).orElse(null);

          List<ClauseRecommendRequestDto.RestoreCategory> restoreCategoryList =
                  fetchRestoreCategories(contractChatId, userId);

          ClauseRecommendRequestDto aiRequest =
                  buildClauseRecommendRequest(ownerDto, contractDocument);
          aiRequest.getOwnerData().setIdentityId(identityId);
          aiRequest.getOwnerData().setOwnerPrecheckId(ownerPrecheckId);
          aiRequest.getOwnerData().setRestoreCategories(restoreCategoryList);

          return aiRequest;
      }

      private List<ClauseRecommendRequestDto.RestoreCategory> fetchRestoreCategories(
              Long contractChatId, Long userId) {
          List<RestoreCategoryVO> restoreCategories =
                  ownerMapper.selectRestoreScope(contractChatId, userId);
          return restoreCategories.stream()
                  .map(
                          cat ->
                                  ClauseRecommendRequestDto.RestoreCategory.builder()
                                          .restoreCategoryId(cat.getRestoreCategoryId())
                                          .restoreCategoryName(cat.getRestoreCategoryName())
                                          .build())
                  .collect(Collectors.toList());
      }

      private void logAiRequest(ClauseRecommendRequestDto aiRequest) {
          try {
              log.info("AI 요청 데이터: {}", objectMapper.writeValueAsString(aiRequest));
          } catch (Exception e) {
              log.error("요청 데이터 로깅 실패", e);
          }
      }

      private void saveAiRecommendation(Long contractChatId, ClauseRecommendResponseDto aiResponse) {
          if (aiResponse != null && aiResponse.isSuccess() && aiResponse.getData() != null) {
              Long round = 1L;

              SpecialContractDocument specialContract =
                      SpecialContractDocument.builder()
                              .contractChatId(contractChatId)
                              .round(round)
                              .totalClauses(aiResponse.getData().getTotalClauses())
                              .clauses(convertClauses(aiResponse.getData().getClauses()))
                              .build();

              SpecialContractDocument savedContract = mongoTemplate.save(specialContract);
              log.info("AI 특약 추천 저장 완료 - 특약 수: {}", savedContract.getTotalClauses());
          }
      }

      private ClauseRecommendRequestDto buildClauseRecommendRequest(
              OwnerPreContractMongoDTO ownerData, ContractDocumentMongoDocument contractDocument) {
          ClauseRecommendRequestDto.OcrData ocrData = buildOcrData(contractDocument);
          ClauseRecommendRequestDto.OwnerData ownerRequestData = buildOwnerData(ownerData);
          ClauseRecommendRequestDto.TenantData tenantData = buildTenantData(ownerData);

          return ClauseRecommendRequestDto.builder()
                  .ocrData(ocrData)
                  .ownerData(ownerRequestData)
                  .tenantData(tenantData)
                  .build();
      }

      private ClauseRecommendRequestDto.OcrData buildOcrData(
              ContractDocumentMongoDocument contractDocument) {
          if (contractDocument == null) {
              return ClauseRecommendRequestDto.OcrData.builder()
                      .extractedAt(null)
                      .fileName(null)
                      .rawText(null)
                      .source(null)
                      .specialTerms(null)
                      .build();
          }

          return ClauseRecommendRequestDto.OcrData.builder()
                  .extractedAt(contractDocument.getExtractedAt())
                  .fileName(contractDocument.getFilename())
                  .rawText(contractDocument.getRawText())
                  .source(contractDocument.getSource())
                  .specialTerms(contractDocument.getSpecialTerms())
                  .build();
      }

      private ClauseRecommendRequestDto.OwnerData buildOwnerData(OwnerPreContractMongoDTO ownerData) {
          ClauseRecommendRequestDto.OwnerData ownerRequestData =
                  ClauseRecommendRequestDto.OwnerData.builder()
                          .contractChatId(ownerData.getContractChatId())
                          .contractDuration(
                                  ownerData.getContractDuration() != null
                                          ? ownerData.getContractDuration().name()
                                          : null)
                          .hasAutoPriceAdjustment(ownerData.getHasAutoPriceAdjustment())
                          .hasConditionLog(ownerData.getHasConditionLog())
                          .hasNotice(
                                  ownerData.getHasNotice() != null
                                          ? ownerData.getHasNotice().name()
                                          : "")
                          .hasPenalty(ownerData.getHasPenalty())
                          .hasPriorityForExtension(ownerData.getHasPriorityForExtension())
                          .identityId(null)
                          .insuranceBurden(
                                  ownerData.getInsuranceBurden() != null
                                          ? ownerData.getInsuranceBurden().name()
                                          : "")
                          .isMortgaged(
                                  ownerData.getMortgaged() != null ? ownerData.getMortgaged() : false)
                          .ownerAccountNumber(ownerData.getOwnerAccountNumber())
                          .ownerBankName(ownerData.getOwnerBankName())
                          .ownerPrecheckId(null)
                          .renewalIntent(
                                  ownerData.getRenewalIntent() != null
                                          ? ownerData.getRenewalIntent().name()
                                          : null)
                          .rentType(
                                  ownerData.getRentType() != null
                                          ? ownerData.getRentType().name()
                                          : null)
                          .requireRentGuaranteeInsurance(ownerData.getRequireRentGuaranteeInsurance())
                          .responseRepairingFixtures(
                                  ownerData.getResponseRepairingFixtures() != null
                                          ? ownerData.getResponseRepairingFixtures().name()
                                          : null)
                          .restoreCategories(null)
                          .wolseInfo(null)
                          .build();

          if (ownerData.getRentType() == RentType.WOLSE
                  && ownerData.getPaymentDueDate() != null
                  && ownerData.getLateFeeInterestRate() != null) {
              ClauseRecommendRequestDto.WolseInfo wolseInfo =
                      ClauseRecommendRequestDto.WolseInfo.builder()
                              .paymentDueDay(ownerData.getPaymentDueDate())
                              .lateFeeInterestRate(ownerData.getLateFeeInterestRate())
                              .build();
              ownerRequestData.setWolseInfo(wolseInfo);
          }

          return ownerRequestData;
      }

      private ClauseRecommendRequestDto.TenantData buildTenantData(
              OwnerPreContractMongoDTO ownerData) {
          return ClauseRecommendRequestDto.TenantData.builder()
                  .contractChatId(ownerData.getContractChatId())
                  .rentType(ownerData.getRentType() != null ? ownerData.getRentType().name() : "")
                  .loanPlan(false)
                  .insurancePlan(false)
                  .expectedMoveInDate("")
                  .contractDuration("")
                  .renewalIntent("")
                  .facilityRepairNeeded(false)
                  .interiorCleaningNeeded(false)
                  .applianceInstallationPlan(false)
                  .hasPet(false)
                  .indoorSmokingPlan(false)
                  .earlyTerminationRisk(false)
                  .checkedAt(LocalDateTime.now().toString())
                  .residentCount(0)
                  .occupation("")
                  .emergencyContact("")
                  .relation("")
                  .build();
      }

      private List<SpecialContractDocument.Clause> convertClauses(
              List<ClauseRecommendResponseDto.Clause> aiClauses) {
          if (aiClauses == null) {
              return new ArrayList<>();
          }

          return aiClauses.stream()
                  .map(
                          aiClause ->
                                  SpecialContractDocument.Clause.builder()
                                          .order(aiClause.getOrder())
                                          .title(aiClause.getTitle())
                                          .content(aiClause.getContent())
                                          .assessment(convertAssessment(aiClause.getAssessment()))
                                          .build())
                  .collect(Collectors.toList());
      }

      private SpecialContractDocument.Assessment convertAssessment(
              ClauseRecommendResponseDto.Assessment aiAssessment) {
          if (aiAssessment == null) {
              return null;
          }

          return SpecialContractDocument.Assessment.builder()
                  .owner(convertEvaluation(aiAssessment.getOwner()))
                  .tenant(convertEvaluation(aiAssessment.getTenant()))
                  .build();
      }

      private SpecialContractDocument.Evaluation convertEvaluation(
              ClauseRecommendResponseDto.PartyAssessment partyAssessment) {
          if (partyAssessment == null) {
              return null;
          }

          return SpecialContractDocument.Evaluation.builder()
                  .level(partyAssessment.getLevel())
                  .reason(partyAssessment.getReason())
                  .build();
      }
}
