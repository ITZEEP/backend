package org.scoula.domain.precontract.service;

import java.util.Optional;

import org.scoula.domain.precontract.dto.tenant.*;
import org.scoula.domain.precontract.enums.RentType;
import org.scoula.domain.precontract.exception.PreContractErrorCode;
import org.scoula.domain.precontract.mapper.TenantPreContractMapper;
import org.scoula.domain.precontract.repository.TenantMongoRepository;
import org.scoula.domain.precontract.vo.TenantJeonseInfoVO;
import org.scoula.domain.precontract.vo.TenantPreContractCheckVO;
import org.scoula.domain.precontract.vo.TenantWolseInfoVO;
import org.scoula.global.common.exception.BusinessException;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class PreContractServiceImpl implements PreContractService {

      private final TenantPreContractMapper tenantMapper;
      private final TenantMongoRepository mongoRepository;

      // =============== 사기 위험도 확인 & 기본 세팅 ==================

      /** {@inheritDoc} */
      @Override
      public Boolean getCheckRisk(Long contractChatId, Long userId) {
          // 0. UserId 검증하기
          Long buyerId =
                  tenantMapper
                          .selectContractBuyerId(contractChatId)
                          .orElseThrow(() -> new BusinessException(PreContractErrorCode.TENANT_USER));

          if (!userId.equals(buyerId)) {
              throw new BusinessException(PreContractErrorCode.TENANT_USER);
          }

          // 1. risk_check에 riskId가 있는지 확인하기
          Optional<Long> riskId = tenantMapper.selectRiskId(contractChatId, userId);

          // 2. iF : 있다면 -> true 값 보내기 / 없다면 -> false 값 보내기
          return riskId.isPresent(); // 사기 위험도 값 받기 (프론트에서 다른 api 호출)
      }

      // =============== 본인인증 하기 ==================

      /** {@inheritDoc} */
      @Override
      @Transactional
      public TenantInitRespDTO saveTenantInfo(Long contractChatId, Long userId) {

          Long buyerId =
                  tenantMapper
                          .selectContractBuyerId(contractChatId)
                          .orElseThrow(() -> new BusinessException(PreContractErrorCode.TENANT_USER));

          if (!userId.equals(buyerId)) {
              throw new BusinessException(PreContractErrorCode.TENANT_USER);
          }

          // 1-1. identity_id 가져오기
          Long identityId =
                  tenantMapper
                          .selectIdentityId(userId)
                          .orElseThrow(
                                  () -> new BusinessException(PreContractErrorCode.TENANT_SELECT));

          // 1-2. 매개변수로 riskId를 찾는다.
          Long riskId =
                  tenantMapper
                          .selectRiskId(contractChatId, buyerId)
                          .orElseThrow(
                                  () -> new BusinessException(PreContractErrorCode.TENANT_SELECT));

          // 1-3. riskType 조회
          String riskType =
                  tenantMapper
                          .selectRiskType(contractChatId, userId)
                          .orElseThrow(
                                  () -> new BusinessException(PreContractErrorCode.TENANT_SELECT));

          // 1-4. 전세/월세 여부도 넣기 위해서 찾기
          String rentType =
                  tenantMapper
                          .selectRentType(contractChatId, userId)
                          .orElseThrow(
                                  () -> new BusinessException(PreContractErrorCode.TENANT_SELECT));

          // 2. tenant_precontract_check 테이블 채우기 (나머지는 Null 값)

          int result1 =
                  tenantMapper.insertPreContractSet(
                          contractChatId, identityId, riskId, rentType, riskType);
          if (result1 != 1) throw new BusinessException(PreContractErrorCode.TENANT_INSERT);

          int result2 = tenantMapper.insertJeonseInfo(contractChatId);
          if (result2 != 1) throw new BusinessException(PreContractErrorCode.TENANT_INSERT);

          int result3 = tenantMapper.insertWolseInfo(contractChatId);
          if (result3 != 1) throw new BusinessException(PreContractErrorCode.TENANT_INSERT);

          // 3. 다음 페이지를 위해서 전세/월세 여부, pet, parking 여부 보내기
          boolean isPet = tenantMapper.selectIsPet(userId, contractChatId);
          boolean istParkingAvailable = tenantMapper.selectIsParking(userId, contractChatId);

          TenantInitRespDTO dto = TenantInitRespDTO.toResp(rentType, isPet, istParkingAvailable);

          return dto;
      }

      // =============== step 1 ==================
      /** {@inheritDoc} */
      @Override
      public Void updateTenantStep1(Long contractChatId, Long userId, TenantStep1DTO step1DTO) {
          // 0. UserId 검증하기
          Long buyerId =
                  tenantMapper
                          .selectContractBuyerId(contractChatId)
                          .orElseThrow(() -> new BusinessException(PreContractErrorCode.TENANT_USER));

          if (!userId.equals(buyerId)) {
              throw new BusinessException(PreContractErrorCode.TENANT_USER);
          }

          // 1. home에서 전세인지 월세인지 값을 가져온다.
          RentType rentType =
                  RentType.valueOf(
                          tenantMapper
                                  .selectRentType(contractChatId, userId)
                                  .orElseThrow(
                                          () ->
                                                  new BusinessException(
                                                          PreContractErrorCode.TENANT_SELECT)));

          // 2. 해당 테이블에 값을 넣는다. (전세 or 월세)
          if (rentType == RentType.JEONSE) {
              // dto -> vo
              TenantJeonseInfoVO vo = TenantJeonseInfoVO.toVO(step1DTO);
              if (vo.getJeonseLoanPlan() == null || vo.getJeonseInsurancePlan() == null) {
                  throw new BusinessException(PreContractErrorCode.TENANT_NULL);
              }
              // mappser
              int result = tenantMapper.updateJeonseInfo(vo, contractChatId);
              if (result != 1) throw new BusinessException(PreContractErrorCode.TENANT_INSERT);
          } else if (rentType == RentType.WOLSE) {
              // dto -> vo
              TenantWolseInfoVO vo = TenantWolseInfoVO.toVO(step1DTO);
              if (vo.getWolseLoanPlan() == null || vo.getWolseInsurancePlan() == null) {
                  throw new BusinessException(PreContractErrorCode.TENANT_NULL);
              }
              // mapper
              int result = tenantMapper.updateWolseInfo(vo, contractChatId);
              if (result != 1) throw new BusinessException(PreContractErrorCode.TENANT_INSERT);
          }

          // 3. dto로 받아오 값들을 update로 저장한다.
          TenantPreContractCheckVO vo = TenantPreContractCheckVO.toStep1VO(step1DTO);

          int updateStep1 = tenantMapper.updateStep1(vo, rentType, userId, contractChatId);

          // 4. 저장된 column을 확인해서 잘 저장됐는지 확인하기 -> exception, 이전에 에러 뜨는지 확인하기
          if (updateStep1 != 1) {
              throw new BusinessException(PreContractErrorCode.TENANT_UPDATE);
          }

          // 5. 다음스텝을 위해서 애완동뭄 여부를 response 값으로 보내기!
          return null;
      }

      /** {@inheritDoc} */
      @Override
      public TenantStep1DTO selectTenantStep1(Long contractChatId, Long userId) {
          // 0. UserId 검증하기
          Long buyerId =
                  tenantMapper
                          .selectContractBuyerId(contractChatId)
                          .orElseThrow(() -> new BusinessException(PreContractErrorCode.TENANT_USER));

          if (!userId.equals(buyerId)) {
              throw new BusinessException(PreContractErrorCode.TENANT_USER);
          }

          // 1. 값을 받아와서 userid 인증확인하고, 값을 dto로 받는다
          TenantStep1DTO dto = tenantMapper.selectStep1(userId, contractChatId);
          //          if (dto == null) throw new
          // BusinessException(PreContractErrorCode.TENANT_SELECT);

          // 2. 값을 반환하기
          return dto;
      }

      // =============== step 2 ==================
      /** {@inheritDoc} */
      @Override
      public Void updateTenantStep2(Long contractChatId, Long userId, TenantStep2DTO step2DTO) {
          // 0. UserId 검증하기
          Long buyerId =
                  tenantMapper
                          .selectContractBuyerId(contractChatId)
                          .orElseThrow(() -> new BusinessException(PreContractErrorCode.TENANT_USER));

          if (!userId.equals(buyerId)) {
              throw new BusinessException(PreContractErrorCode.TENANT_USER);
          }

          // 1. userid, 정보 가져와서 dto 값으로 저장하기 -> pet이 있고, 없고에 따라서 알아서 저장된다.
          TenantPreContractCheckVO vo = TenantPreContractCheckVO.toStep2VO(step2DTO);

          int result = tenantMapper.updateStep2(vo, userId, contractChatId);
          if (result != 1) throw new BusinessException(PreContractErrorCode.TENANT_UPDATE);

          if (Boolean.TRUE.equals(vo.getHasParking())) {
              if (step2DTO.getHasParking() == null || step2DTO.getParkingCount() < 1) {
                  throw new BusinessException(PreContractErrorCode.TENANT_NULL);
              }
          }
          // 반려동물 관련 추가 처리
          if (Boolean.TRUE.equals(vo.getHasPet())) {
              if (step2DTO.getHasPet() == null
                      || step2DTO.getPetInfo() == null
                      || step2DTO.getPetCount() < 1) {
                  throw new BusinessException(PreContractErrorCode.TENANT_NULL);
              }
          }


          return null;
      }

      /** {@inheritDoc} */
      @Override
      public TenantStep2DTO selectTenantStep2(Long contractChatId, Long userId) {
          // 0. UserId 검증하기
          Long buyerId =
                  tenantMapper
                          .selectContractBuyerId(contractChatId)
                          .orElseThrow(() -> new BusinessException(PreContractErrorCode.TENANT_USER));

          if (!userId.equals(buyerId)) {
              throw new BusinessException(PreContractErrorCode.TENANT_USER);
          }
          // 1. 인증된 값으로 조회하기
          TenantStep2DTO dto = tenantMapper.selectStep2(userId, contractChatId);
          if (dto == null) throw new BusinessException(PreContractErrorCode.TENANT_SELECT);

          // 2. 조회된 값 반환하기
          return dto;
      }

      //  =============== step 3 ==================
      /** {@inheritDoc} */
      @Override
      public Void updateTenantStep3(Long contractChatId, Long userId, TenantStep3DTO step3DTO) {
          // 0. UserId 검증하기
          Long buyerId =
                  tenantMapper
                          .selectContractBuyerId(contractChatId)
                          .orElseThrow(() -> new BusinessException(PreContractErrorCode.TENANT_USER));

          if (!userId.equals(buyerId)) {
              throw new BusinessException(PreContractErrorCode.TENANT_USER);
          }

          // 인증된 값으로 저장하기
          TenantPreContractCheckVO vo = TenantPreContractCheckVO.toStep3VO(step3DTO);

          int result = tenantMapper.updateStep3(vo, userId, contractChatId);
          if (result != 1) throw new BusinessException(PreContractErrorCode.TENANT_UPDATE);

          return null;
      }

      /** {@inheritDoc} */
      @Override
      public TenantStep3DTO selectTenantStep3(Long contractChatId, Long userId) {
          // 0. UserId 검증하기
          Long buyerId =
                  tenantMapper
                          .selectContractBuyerId(contractChatId)
                          .orElseThrow(() -> new BusinessException(PreContractErrorCode.TENANT_USER));

          if (!userId.equals(buyerId)) {
              throw new BusinessException(PreContractErrorCode.TENANT_USER);
          }

          // 1. 보증된 값으로 조회하기
          TenantStep3DTO dto = tenantMapper.selectStep3(userId, contractChatId);
          if (dto == null) throw new BusinessException(PreContractErrorCode.TENANT_SELECT);

          // 2. 조회된 값을 반환하기
          return dto;
      }

      // =============== 최종 ==================

      /** {@inheritDoc} */
      @Override
      public TenantPreContractDTO selectTenantPreCon(Long contractChatId, Long userId) {
          // 0. UserId 검증하기
          Long buyerId =
                  tenantMapper
                          .selectContractBuyerId(contractChatId)
                          .orElseThrow(() -> new BusinessException(PreContractErrorCode.TENANT_USER));

          if (!userId.equals(buyerId)) {
              throw new BusinessException(PreContractErrorCode.TENANT_USER);
          }

          // 1. 검증된 결과로 조회하기
          TenantPreContractDTO dto = tenantMapper.selectPreCon(userId, contractChatId);
          if (dto == null) throw new BusinessException(PreContractErrorCode.TENANT_SELECT);

          // 2. 조회된 값을 반환하기
          return dto;
      }

      /** {@inheritDoc} */
      @Override
      public Void saveMongoDB(Long contractChatId, Long userId) {
          // 0. UserId 검증하기
          Long buyerId =
                  tenantMapper
                          .selectContractBuyerId(contractChatId)
                          .orElseThrow(() -> new BusinessException(PreContractErrorCode.TENANT_USER));

          if (!userId.equals(buyerId)) {
              throw new BusinessException(PreContractErrorCode.TENANT_USER);
          }

          // 1. 매퍼에서 보낼것들을 조회해오기
          TenantMongoDTO dto = tenantMapper.selectMongo(userId, contractChatId);
          if (dto == null) throw new BusinessException(PreContractErrorCode.TENANT_SELECT);

          // 2. 몽고 DB에 저장하기
          try {
              mongoRepository.insert(dto);
          } catch (DataAccessException e) {
              throw new BusinessException(PreContractErrorCode.TENANT_INSERT, e);
          }

          return null;
      }
}
