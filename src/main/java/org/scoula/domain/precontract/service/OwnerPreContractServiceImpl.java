package org.scoula.domain.precontract.service;

import java.util.List;
import java.util.Optional;

import org.scoula.domain.precontract.dto.owner.*;
import org.scoula.domain.precontract.enums.RentType;
import org.scoula.domain.precontract.exception.OwnerPreContractErrorCode;
import org.scoula.domain.precontract.mapper.OwnerPreContractMapper;
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

          // 5. 반환
          return OwnerInitRespDTO.toResp(rentType);
      }

      @Override
      public Void updateOwnerContractStep1(
              Long contractChatId, Long userId, OwnerContractStep1DTO contractStep1DTO) {
          validateUser(contractChatId, userId, true);
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
              Long contractChatId, Long userId, OwnerContractStep2DTO contractStep2DTO) {
          validateUser(contractChatId, userId, true);

          // 계약 조건 업데이트
          int updated = ownerMapper.updateContractSub2(contractStep2DTO, contractChatId, userId);
          if (updated != 1) throw new BusinessException(OwnerPreContractErrorCode.OWNER_UPDATE);

          // 원상복구 범위 삭제 후 재삽입
          Long ownerPrecheckId = contractStep2DTO.getOwnerPrecheckId();
          ownerMapper.deleteRestoreScopes(ownerPrecheckId);

          List<RestoreCategoryVO> categories = contractStep2DTO.getRestoreCategories();
          int inserted = 0;
          for (RestoreCategoryVO category : categories) {
              inserted +=
                      ownerMapper.insertRestoreScope(
                              ownerPrecheckId, category.getRestoreCategoryId());
          }

          if (inserted != categories.size()) {
              throw new BusinessException(OwnerPreContractErrorCode.OWNER_UPDATE);
          }

          return null;
      }

      @Override
      public OwnerContractStep2DTO selectOwnerContractStep2(Long contractChatId, Long userId) {
          validateUser(contractChatId, userId, true);

          OwnerContractStep2DTO contractStep2 =
                  ownerMapper
                          .selectContractSub2(contractChatId, userId)
                          .orElseThrow(
                                  () ->
                                          new BusinessException(
                                                  OwnerPreContractErrorCode.OWNER_SELECT));
          List<RestoreCategoryVO> restoreList =
                  ownerMapper.selectRestoreScope(contractChatId, userId);

          contractStep2.setRestoreCategories(restoreList);
          return contractStep2;
      }

      @Override
      public Void updateOwnerLivingStep1(Long contractChatId, Long userId, OwnerLivingStep1DTO dto) {
          validateUser(contractChatId, userId, true);
          int result = ownerMapper.updateLivingSub1(dto, contractChatId, userId);
          if (result != 1) throw new BusinessException(OwnerPreContractErrorCode.OWNER_UPDATE);
          return null;
      }

      @Override
      public OwnerLivingStep1DTO selectOwnerLivingStep1(Long contractChatId, Long userId) {
          validateUser(contractChatId, userId, true);
          return ownerMapper
                  .selectLivingSub1(contractChatId, userId)
                  .orElseThrow(() -> new BusinessException(OwnerPreContractErrorCode.OWNER_SELECT));
      }

      @Override
      public void saveContractDocument(
              Long contractChatId, Long userId, ContractDocumentDTO contractDocumentDTO) {}

      @Override
      public Void updateOwnerLivingStep2(Long contractChatId, Long userId) {
          validateUser(contractChatId, userId, true);

          String rentTypeStr =
                  ownerMapper
                          .selectRentType(contractChatId, userId)
                          .orElseThrow(
                                  () ->
                                          new BusinessException(
                                                  OwnerPreContractErrorCode.OWNER_SELECT));
          RentType rentType = RentType.valueOf(rentTypeStr);

          int result;

          if (rentType == RentType.JEONSE) {
              OwnerLivingStep2JeonseDTO dto =
                      ownerMapper
                              .selectLivingJeonse(contractChatId, userId)
                              .orElseThrow(
                                      () ->
                                              new BusinessException(
                                                      OwnerPreContractErrorCode.OWNER_SELECT));
              result = ownerMapper.updateLivingJeonse(dto, contractChatId, userId);
          } else if (rentType == RentType.WOLSE) {
              OwnerLivingStep2WolseDTO dto =
                      ownerMapper
                              .selectLivingWolse(contractChatId, userId)
                              .orElseThrow(
                                      () ->
                                              new BusinessException(
                                                      OwnerPreContractErrorCode.OWNER_SELECT));
              result = ownerMapper.updateLivingWolse(dto, contractChatId, userId);
          } else {
              throw new BusinessException(OwnerPreContractErrorCode.ENUM_VALUE_OF);
          }

          if (result != 1) throw new BusinessException(OwnerPreContractErrorCode.OWNER_UPDATE);
          return null;
      }

      @Override
      public Object selectOwnerLivingStep2(Long contractChatId, Long userId) {
          String rentType =
                  ownerMapper
                          .selectRentType(contractChatId, userId)
                          .orElseThrow(
                                  () ->
                                          new BusinessException(
                                                  OwnerPreContractErrorCode.OWNER_SELECT));

          if (RentType.JEONSE.name().equals(rentType)) {
              return ownerMapper.selectLivingJeonse(contractChatId, userId);
          } else if (RentType.WOLSE.name().equals(rentType)) {
              return ownerMapper.selectLivingWolse(contractChatId, userId);
          } else {
              throw new BusinessException(OwnerPreContractErrorCode.OWNER_SELECT);
          }
      }

      @Override
      public OwnerPreContractDTO selectOwnerPreContract(Long contractChatId, Long userId) {
          validateUser(contractChatId, userId, true);
          return ownerMapper
                  .selectSummary(contractChatId)
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
