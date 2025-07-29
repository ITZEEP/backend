package org.scoula.domain.precontract.service;

import org.scoula.domain.precontract.dto.TenantPreContractDTO;
import org.scoula.domain.precontract.dto.TenantStep1DTO;
import org.scoula.domain.precontract.dto.TenantStep2DTO;
import org.scoula.domain.precontract.dto.TenantStep3DTO;
import org.scoula.domain.precontract.enums.RentType;
import org.scoula.domain.precontract.exception.PreContractErrorCode;
import org.scoula.domain.precontract.mapper.TenantPreContractMapper;
import org.scoula.domain.precontract.vo.TenantJeonseInfoVO;
import org.scoula.domain.precontract.vo.TenantPreContractCheckVO;
import org.scoula.domain.precontract.vo.TenantWolseInfoVO;
import org.scoula.global.auth.dto.CustomUserDetails;
import org.scoula.global.common.exception.BusinessException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// 처음에 정보를 보내서 (전,월세 / 펫 여부) 그걸 로컬 스토리지에 저장하게 한다
// 동재님이 받아오는 값에 있는 메서드가 두개가 섞여 있는것 같은데 그거 정리하기

@Service
@RequiredArgsConstructor
@Log4j2
public class PreContractServiceImpl implements PreContractService {

      private final TenantPreContractMapper tenantMapper;

      // =============== 사기 위험도 확인 & 기본 세팅 ==================

      /** {@inheritDoc} */
      @Override
      public Boolean getCheckRisk(Long contractChatId, Long userId) { // 리턴값을 dto로 하기
          // 1. risk_check에 riskId가 있는지 확인하기
          Long riskId = tenantMapper.selectRiskId(contractChatId, userId);
          // 2. if
          //    있다면 -> true 값 보내기
          //    없다면 -> false 값 보내기
          if (riskId != null) {
              // 사기 위험도 값 받기
              return true;
          } else {
              throw new BusinessException(PreContractErrorCode.TENANT_RISK);
              // 사기 위험도 페이지로 Redirect 하기
          }
      }

      /** {@inheritDoc} */
      @Override
      @Transactional
      public String saveTenantInfo(Long contractChatId, Long buyerId) {
          // 유저 아이디 확인하기
          CustomUserDetails userDetails =
                  (CustomUserDetails)
                          SecurityContextHolder.getContext().getAuthentication().getPrincipal();
          Long userId = userDetails.getUserId();

          if (userId != buyerId) {
              throw new BusinessException(PreContractErrorCode.TENANT_USER);
          }

          //          // 0. identity_verification에 행을 추가한다.
          //          int idResult = tenantMapper.insertIdentityVerification(userId);
          //          if (idResult != 1) throw new
          // BusinessException(PreContractErrorCode.TENANT_INSERT);

          // 0. identity_id 가져오기
          Long identityId = tenantMapper.selectIdentityId(userId);

          // 1. 매개변수로 riskId를 찾는다.
          Long riskId = tenantMapper.selectRiskId(contractChatId, buyerId);
          if (riskId == null) throw new BusinessException(PreContractErrorCode.TENANT_INSERT);

          String riskType = String.valueOf(tenantMapper.selectRiskType(contractChatId, userId));
          if (riskType == null) throw new BusinessException(PreContractErrorCode.TENANT_INSERT);

          // 1.1 전세/월세 여부도 넣기 위해서 찾기
          String rentType = tenantMapper.selectLeaseType(buyerId, contractChatId);

          // 2. tenant_precontract_check 테이블 채우기 (나머지는 Null 값)
          // 전월세 저장하기
          int result =
                  tenantMapper.insertPreContractSet(
                          contractChatId, identityId, riskId, rentType, riskType);
          if (result != 1) throw new BusinessException(PreContractErrorCode.TENANT_INSERT);

          // 다음 페이지를 위해서 전세/월세 여부 보내기
          return rentType;
      }

      // =============== step 1 ==================
      /** {@inheritDoc} */
      @Override
      public Boolean updateTenantStep1(Long contractChatId, Long userId, TenantStep1DTO step1DTO) {
          // 1. home에서 전세인지 월세인지 값을 가져온다. : lease_type
          RentType rentType = RentType.valueOf(tenantMapper.selectLeaseType(userId, contractChatId));

          // 2. 해당 테이블에 값을 넣는다. (전세 or 월세)
          if (rentType == RentType.JEONSE) {
              // dto -> vo
              TenantJeonseInfoVO vo = TenantJeonseInfoVO.toVO(contractChatId, step1DTO);
              // mappser
              int result = tenantMapper.insertJeonseInfo(vo);
              if (result != 1) throw new BusinessException(PreContractErrorCode.TENANT_INSERT);
          } else if (rentType == RentType.WOLSE) {
              // dto -> vo
              TenantWolseInfoVO vo = TenantWolseInfoVO.toVO(contractChatId, step1DTO);
              // mapper
              int result = tenantMapper.insertWolseInfo(vo);
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
          return tenantMapper.selectIsPet(userId, contractChatId);
      }

      /** {@inheritDoc} */
      @Override
      public TenantStep1DTO selectTenantStep1(Long contractChatId, Long userId) {
          // 1. 값을 받아와서 userid 인증확인하고, 값을 dto로 받는다
          TenantStep1DTO dto = tenantMapper.selectStep1(userId, contractChatId);

          // 2. 값을 반환하기
          return dto;
      }

      // =============== step 2 ==================
      /** {@inheritDoc} */
      @Override
      public Void updateTenantStep2(Long contractChatId, Long userId, TenantStep2DTO step2DTO) {
          // 1. userid, 정보 가져와서 dto 값으로 저장하기
          // -> pet이 있고, 없고에 따라서 알아서 저장된다.
          TenantPreContractCheckVO vo = TenantPreContractCheckVO.toStep2VO(step2DTO);
          int result = tenantMapper.updateStep2(vo, userId, contractChatId);
          if (result != 1) throw new BusinessException(PreContractErrorCode.TENANT_UPDATE);
          return null;
      }

      /** {@inheritDoc} */
      @Override
      public TenantStep2DTO selectTenantStep2(Long contractChatId, Long userId) {
          // 1. 인증된 값으로 조회하기
          TenantStep2DTO dto = tenantMapper.selectStep2(userId, contractChatId);
          // 2. 조회된 값 반환하기
          return dto;
      }

      //  =============== step 3 ==================
      /** {@inheritDoc} */
      @Override
      public Void updateTenantStep3(Long contractChatId, Long userId, TenantStep3DTO step3DTO) {
          // 인증된 값으로 저장하기
          TenantPreContractCheckVO vo = TenantPreContractCheckVO.toStep3VO(step3DTO);
          int result = tenantMapper.updateStep3(vo, userId, contractChatId);
          if (result != 1) throw new BusinessException(PreContractErrorCode.TENANT_UPDATE);
          return null;
      }

      /** {@inheritDoc} */
      @Override
      public TenantStep3DTO selectTenantStep3(Long contractChatId, Long userId) {
          // 1. 보증된 값으로 조회하기
          TenantStep3DTO dto = tenantMapper.selectStep3(userId, contractChatId);
          // 2. 조회된 값을 반환하기
          return dto;
      }

      // =============== 최종 ==================

      /** {@inheritDoc} */
      @Override
      public TenantPreContractDTO selectTenantPreCon(Long contractChatId, Long userId) {
          // 1. 검증된 결과로 조회하기
          TenantPreContractDTO dto = tenantMapper.selectPreCon(userId, contractChatId);
          // 2. 조회된 값을 반환하기
          return dto;
      }
}
