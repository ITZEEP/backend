package org.scoula.domain.precontract.service;

import java.util.ArrayList;
import java.util.List;

import org.scoula.domain.chat.dto.ai.ClauseImproveRequestDto;
import org.scoula.domain.precontract.document.ContractDocumentMongoDocument;
import org.scoula.domain.precontract.dto.owner.OwnerPreContractMongoDTO;
import org.scoula.domain.precontract.dto.tenant.TenantMongoDTO;
import org.scoula.domain.precontract.mapper.OwnerPreContractMapper;
import org.scoula.domain.precontract.mapper.TenantPreContractMapper;
import org.scoula.domain.precontract.repository.ContractDocumentMongoRepository;
import org.scoula.domain.precontract.vo.RestoreCategoryVO;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class PreContractDataServiceImpl implements PreContractDataService {

      private final OwnerPreContractMapper ownerMapper;
      private final TenantPreContractMapper tenantMapper;
      private final ContractDocumentMongoRepository contractDocumentMongoRepository;

      @Override
      public ClauseImproveRequestDto.OwnerData fetchOwnerData(Long contractChatId) {
          log.info("Owner 데이터 조회 시작 - contractChatId: {}", contractChatId);

          // Owner ID 조회
          Long ownerId =
                  ownerMapper
                          .selectContractOwnerId(contractChatId)
                          .orElseThrow(
                                  () ->
                                          new IllegalArgumentException(
                                                  "Owner를 찾을 수 없습니다. contractChatId: "
                                                          + contractChatId));

          // Owner 기본 데이터 조회
          OwnerPreContractMongoDTO ownerDto = ownerMapper.selectMongo(contractChatId, ownerId);
          if (ownerDto == null) {
              throw new IllegalArgumentException(
                      "Owner 데이터를 찾을 수 없습니다. contractChatId: " + contractChatId);
          }

          // 추가 정보 조회
          Long identityId = ownerMapper.selectIdentityId(contractChatId).orElse(null);
          Long ownerPrecheckId =
                  ownerMapper.selectOwnerPrecheckId(contractChatId, ownerId).orElse(null);

          // 복구 범위 조회
          List<ClauseImproveRequestDto.RestoreCategory> restoreCategoryList =
                  convertRestoreCategories(ownerMapper.selectRestoreScope(contractChatId, ownerId));

          // OwnerData 빌드
          ClauseImproveRequestDto.OwnerData ownerData =
                  buildOwnerData(
                          ownerDto, contractChatId, identityId, ownerPrecheckId, restoreCategoryList);

          // 전세/월세 정보 설정
          setRentTypeInfo(ownerData, ownerDto);

          log.info("Owner 데이터 조회 완료 - contractChatId: {}, ownerId: {}", contractChatId, ownerId);
          return ownerData;
      }

      @Override
      public ClauseImproveRequestDto.TenantData fetchTenantData(Long contractChatId) {
          log.info("Tenant 데이터 조회 시작 - contractChatId: {}", contractChatId);

          // Buyer ID 조회
          Long buyerId =
                  tenantMapper
                          .selectContractBuyerId(contractChatId)
                          .orElseThrow(
                                  () ->
                                          new IllegalArgumentException(
                                                  "Buyer를 찾을 수 없습니다. contractChatId: "
                                                          + contractChatId));

          // Tenant 기본 데이터 조회
          TenantMongoDTO tenantDto = tenantMapper.selectMongo(buyerId, contractChatId);
          if (tenantDto == null) {
              throw new IllegalArgumentException(
                      "Tenant 데이터를 찾을 수 없습니다. contractChatId: " + contractChatId);
          }

          Long identityId = tenantMapper.selectIdentityId(buyerId).orElse(null);

          ClauseImproveRequestDto.TenantData tenantData =
                  buildTenantData(tenantDto, contractChatId, identityId);

          log.info("Tenant 데이터 조회 완료 - contractChatId: {}, buyerId: {}", contractChatId, buyerId);
          return tenantData;
      }

      @Override
      public ClauseImproveRequestDto.OcrData fetchOcrData(Long contractChatId) {
          log.info("OCR 데이터 조회 시작 - contractChatId: {}", contractChatId);

          // Owner ID 조회 (OCR 문서는 owner가 업로드)
          Long ownerId =
                  ownerMapper
                          .selectContractOwnerId(contractChatId)
                          .orElseThrow(
                                  () ->
                                          new IllegalArgumentException(
                                                  "Owner를 찾을 수 없습니다. contractChatId: "
                                                          + contractChatId));

          // MongoDB에서 계약서 문서 조회
          ContractDocumentMongoDocument contractDocument =
                  contractDocumentMongoRepository.findByContractChatIdAndUserId(
                          contractChatId, ownerId);

          if (contractDocument == null) {
              log.info("OCR 데이터가 없습니다. null 값으로 반환 - contractChatId: {}", contractChatId);
              // OCR 데이터가 없는 경우 기본값 반환
              return ClauseImproveRequestDto.OcrData.builder()
                      .extractedAt(null)
                      .fileName(null)
                      .rawText(null)
                      .source(null)
                      .specialTerms(new ArrayList<>())
                      .build();
          }

          ClauseImproveRequestDto.OcrData ocrData =
                  ClauseImproveRequestDto.OcrData.builder()
                          .extractedAt(contractDocument.getExtractedAt())
                          .fileName(contractDocument.getFilename())
                          .rawText(contractDocument.getRawText())
                          .source(contractDocument.getSource())
                          .specialTerms(
                                  contractDocument.getSpecialTerms() != null
                                          ? contractDocument.getSpecialTerms()
                                          : new ArrayList<>())
                          .build();

          log.info(
                  "OCR 데이터 조회 완료 - contractChatId: {}, fileName: {}",
                  contractChatId,
                  contractDocument.getFilename());
          return ocrData;
      }

      private List<ClauseImproveRequestDto.RestoreCategory> convertRestoreCategories(
              List<RestoreCategoryVO> restoreCategories) {
          return restoreCategories.stream()
                  .map(
                          category ->
                                  ClauseImproveRequestDto.RestoreCategory.builder()
                                          .restoreCategoryId(category.getRestoreCategoryId())
                                          .restoreCategoryName(category.getRestoreCategoryName())
                                          .build())
                  .collect(java.util.stream.Collectors.toList());
      }

      private ClauseImproveRequestDto.JeonseInfo buildJeonseInfo(OwnerPreContractMongoDTO ownerDto) {
          return ClauseImproveRequestDto.JeonseInfo.builder()
                  .allowJeonseRightRegistration(ownerDto.getAllowJeonseRightRegistration())
                  .build();
      }

      private ClauseImproveRequestDto.WolseInfo buildWolseInfo(OwnerPreContractMongoDTO ownerDto) {
          return ClauseImproveRequestDto.WolseInfo.builder()
                  .paymentDueDay(ownerDto.getPaymentDueDate())
                  .lateFeeInterestRate(ownerDto.getLateFeeInterestRate())
                  .build();
      }

      private String safeEnumToString(Enum<?> enumValue) {
          return enumValue != null ? enumValue.name() : null;
      }

      private String safeEnumToStringWithDefault(Enum<?> enumValue, String defaultValue) {
          return enumValue != null ? enumValue.name() : defaultValue;
      }

      private String safeBooleanToString(Boolean value, String defaultValue) {
          return value != null ? value.toString() : defaultValue;
      }

      private String safeObjectToString(Object value, String defaultValue) {
          return value != null ? value.toString() : defaultValue;
      }

      private ClauseImproveRequestDto.OwnerData buildOwnerData(
              OwnerPreContractMongoDTO ownerDto,
              Long contractChatId,
              Long identityId,
              Long ownerPrecheckId,
              List<ClauseImproveRequestDto.RestoreCategory> restoreCategoryList) {

          return ClauseImproveRequestDto.OwnerData.builder()
                  .checkedAt(java.time.LocalDateTime.now().toString())
                  .contractChatId(contractChatId)
                  .contractDuration(safeEnumToString(ownerDto.getContractDuration()))
                  .hasAutoPriceAdjustment(ownerDto.getHasAutoPriceAdjustment())
                  .hasConditionLog(ownerDto.getHasConditionLog())
                  .hasNotice(safeEnumToStringWithDefault(ownerDto.getHasNotice(), ""))
                  .hasPenalty(ownerDto.getHasPenalty())
                  .hasPriorityForExtension(ownerDto.getHasPriorityForExtension())
                  .identityId(identityId)
                  .insuranceBurden(safeEnumToStringWithDefault(ownerDto.getInsuranceBurden(), ""))
                  .isMortgaged(ownerDto.getMortgaged() != null ? ownerDto.getMortgaged() : false)
                  .ownerAccountNumber(ownerDto.getOwnerBankAccountNumber())
                  .ownerBankName(ownerDto.getOwnerBankName())
                  .ownerPrecheckId(ownerPrecheckId)
                  .renewalIntent(safeEnumToString(ownerDto.getRenewalIntent()))
                  .rentType(safeEnumToString(ownerDto.getRentType()))
                  .requireRentGuaranteeInsurance(ownerDto.getRequireRentGuaranteeInsurance())
                  .responseRepairingFixtures(
                          safeEnumToString(ownerDto.getResponseRepairingFixtures()))
                  .restoreCategories(restoreCategoryList)
                  .build();
      }

      private void setRentTypeInfo(
              ClauseImproveRequestDto.OwnerData ownerData, OwnerPreContractMongoDTO ownerDto) {
          String rentType = safeEnumToString(ownerDto.getRentType());
          if ("JEONSE".equals(rentType)) {
              ownerData.setJeonseInfo(buildJeonseInfo(ownerDto));
          } else if ("WOLSE".equals(rentType)) {
              ownerData.setWolseInfo(buildWolseInfo(ownerDto));
          }
      }

      private ClauseImproveRequestDto.TenantData buildTenantData(
              TenantMongoDTO tenantDto, Long contractChatId, Long identityId) {

          return ClauseImproveRequestDto.TenantData.builder()
                  .applianceInstallationPlan(
                          safeBooleanToString(tenantDto.getApplianceInstallationPlan(), "NO"))
                  .checkedAt(
                          safeObjectToString(
                                  tenantDto.getCheckedAt(), java.time.LocalDateTime.now().toString()))
                  .contractChatId(contractChatId)
                  .contractDuration(
                          tenantDto.getContractDuration() != null
                                  ? tenantDto.getContractDuration()
                                  : "")
                  .earlyTerminationRisk(
                          safeBooleanToString(tenantDto.getEarlyTerminationRisk(), "NO"))
                  .emergencyContact(tenantDto.getEmergencyContact())
                  .expectedMoveInDate(safeObjectToString(tenantDto.getExpectedMoveInDate(), ""))
                  .facilityRepairNeeded(
                          safeBooleanToString(tenantDto.getFacilityRepairNeeded(), "NO"))
                  .hasPet(safeBooleanToString(tenantDto.getHasPet(), "NO"))
                  .identityId(identityId)
                  .indoorSmokingPlan(safeBooleanToString(tenantDto.getIndoorSmokingPlan(), "NO"))
                  .insurancePlan(safeBooleanToString(tenantDto.getInsurancePlan(), "NO"))
                  .interiorCleaningNeeded(
                          safeBooleanToString(tenantDto.getInteriorCleaningNeeded(), "NO"))
                  .loanPlan(safeBooleanToString(tenantDto.getLoanPlan(), "NO"))
                  .occupation(tenantDto.getOccupation())
                  .petCount(tenantDto.getPetCount() != null ? tenantDto.getPetCount().intValue() : 0)
                  .petInfo(tenantDto.getPetInfo())
                  .relation(tenantDto.getRelation())
                  .renewalIntent(
                          tenantDto.getRenewalIntent() != null ? tenantDto.getRenewalIntent() : "")
                  .rentType(tenantDto.getRentType() != null ? tenantDto.getRentType() : "")
                  .requestToOwner(tenantDto.getRequestToOwner())
                  .residentCount(tenantDto.getResidentCount())
                  .build();
      }
}
