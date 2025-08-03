package org.scoula.domain.precontract.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.scoula.domain.precontract.dto.owner.*;
import org.scoula.domain.precontract.enums.RentType;
import org.scoula.domain.precontract.exception.OwnerPreContractErrorCode;
import org.scoula.domain.precontract.mapper.OwnerPreContractMapper;
import org.scoula.domain.precontract.vo.OwnerJeonseInfoVO;
import org.scoula.domain.precontract.vo.OwnerWolseInfoVO;
import org.scoula.domain.precontract.vo.RestoreCategoryVO;
import org.scoula.global.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class OwnerPreContractServiceImpl implements OwnerPreContractService {

      private final OwnerPreContractMapper ownerMapper;

      @Override
      @Transactional
      public OwnerInitRespDTO saveOwnerInfo(Long contractChatId, Long ownerId) {
          // 1. 권한 검증
          validateUser(contractChatId, ownerId, false);

          // 2. Identity ID 조회
          Long identityId =
                  ownerMapper
                          .selectIdentityId(ownerId)
                          .orElseThrow(
                                  () ->
                                          new BusinessException(
                                                  OwnerPreContractErrorCode.OWNER_SELECT));

          // 3. Rent Type 조회
          String rentType =
                  ownerMapper
                          .selectRentType(contractChatId, ownerId)
                          .orElseThrow(
                                  () ->
                                          new BusinessException(
                                                  OwnerPreContractErrorCode.OWNER_SELECT));

          // 4. INSERT 기본 세팅
          int inserted = ownerMapper.insertOwnerPreContractSet(contractChatId, identityId, rentType);
          if (inserted == 0) {
              throw new BusinessException(OwnerPreContractErrorCode.OWNER_INSERT);
          }

          // 5. 전/월세 조건에 따라 초기화 insert (contract_chat_id만 삽입, 나머지는 null)
          if ("JEONSE".equalsIgnoreCase(rentType)) {
              OwnerJeonseInfoVO jeonseInfo =
                      OwnerJeonseInfoVO.builder().contractChatId(contractChatId).build();
              ownerMapper.insertJeonseInfo(jeonseInfo);
          } else if ("WOLSE".equalsIgnoreCase(rentType)) {
              OwnerWolseInfoVO wolseInfo =
                      OwnerWolseInfoVO.builder().contractChatId(contractChatId).build();
              ownerMapper.insertWolseInfo(wolseInfo);
          }

          // 6. 반환
          return OwnerInitRespDTO.toResp(rentType);
      }

      @Override
      public Void updateOwnerContractStep1(
              Long contractChatId, Long userId, OwnerContractStep1DTO contractStep1DTO) {
          validateUser(contractChatId, userId, true);

          contractStep1DTO.setCheckedAt(LocalDateTime.now());
          int result = ownerMapper.updateContractSub1(contractChatId, contractStep1DTO);
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
          dto.setCheckedAt(LocalDateTime.now());
          ownerMapper.updateContractSub2(dto, contractChatId, userId);

          // 2. ownerPrecheckId 조회
          Long ownerPrecheckId =
                  ownerMapper
                          .selectOwnerPrecheckId(contractChatId, userId)
                          .orElseThrow(
                                  () ->
                                          new BusinessException(
                                                  OwnerPreContractErrorCode.OWNER_SELECT));

          // 3. 복구 범위 upsert - restoreCategoryIds 우선, 없으면 restoreCategories(name) 사용
          if (dto.getRestoreCategoryIds() != null && !dto.getRestoreCategoryIds().isEmpty()) {
              for (Long categoryId : dto.getRestoreCategoryIds()) {
                  ownerMapper.upsertRestoreScope(ownerPrecheckId, categoryId);
              }
          } else if (dto.getRestoreCategories() != null && !dto.getRestoreCategories().isEmpty()) {
              for (String categoryName : dto.getRestoreCategories()) {
                  Long categoryId = ownerMapper.selectRestoreCategoryIdByName(categoryName);
                  if (categoryId == null) {
                      throw new BusinessException(
                              OwnerPreContractErrorCode.ENUM_VALUE_OF); // 혹은 새 에러코드 정의
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

          // 복구 범위 조회 → 이름만 추출
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

          dto.setCheckedAt(LocalDateTime.now());
          int updated = ownerMapper.updateLivingSub1(dto, contractChatId, userId);
          if (updated != 1) throw new BusinessException(OwnerPreContractErrorCode.OWNER_UPDATE);

          RentType rentType = RentType.valueOf(dto.getRentType());

          // rentType에 따라 전세/월세 조건 저장
          if (rentType == RentType.JEONSE) {
              Boolean allow = dto.getAllowJeonseRightRegistration();
              if (allow == null)
                  throw new BusinessException(OwnerPreContractErrorCode.OWNER_MISSING_DATA);
              int jeonseResult = ownerMapper.updateLivingJeonse(contractChatId, userId, allow);
              if (jeonseResult != 1)
                  throw new BusinessException(OwnerPreContractErrorCode.OWNER_UPDATE);
          } else if (rentType == RentType.WOLSE) {
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

          if (dto.getRentType() == null) {
              throw new BusinessException(OwnerPreContractErrorCode.RENT_TYPE_MISSING);
          }

          RentType rentType = RentType.valueOf(dto.getRentType());

          if (rentType == RentType.JEONSE) {
              OwnerJeonseInfoVO vo =
                      ownerMapper
                              .selectLivingJeonse(contractChatId, userId)
                              .orElseThrow(
                                      () ->
                                              new BusinessException(
                                                      OwnerPreContractErrorCode.OWNER_SELECT));
              dto.setAllowJeonseRightRegistration(vo.getAllowJeonseRightRegistration());
          } else if (rentType == RentType.WOLSE) {
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
      public void saveContractDocument(
              Long contractChatId, Long userId, ContractDocumentDTO contractDocumentDTO) {}

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
}
