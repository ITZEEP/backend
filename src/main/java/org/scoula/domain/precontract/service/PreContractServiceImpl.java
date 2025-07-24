package org.scoula.domain.precontract.service;

import org.scoula.domain.precontract.dto.TenantPreContractDTO;
import org.scoula.domain.precontract.exception.PreContractErrorCode;
import org.scoula.domain.precontract.mapper.TenantPreContractMapper;
import org.scoula.domain.precontract.vo.TenantJeonseInfoVO;
import org.scoula.domain.precontract.vo.TenantPreContractCheckVO;
import org.scoula.domain.precontract.vo.TenantPreContractCheckVO.RentType;
import org.scoula.domain.precontract.vo.TenantWolseInfoVO;
import org.scoula.global.common.exception.BusinessException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Log4j2
public class PreContractServiceImpl implements PreContractService {

      private final TenantPreContractMapper tenantMapper;

      /** {@inheritDoc} */
      @Override
      @Transactional
      public TenantPreContractDTO saveTenantInfo(TenantPreContractDTO request) {
          // 1. RentType으로 전세인지 월세인지 판별한다
          RentType rentType;
          try {
              RentType.valueOf(request.getRentType());
          } catch (IllegalArgumentException e) {
              throw new BusinessException(PreContractErrorCode.ENUM_VALUE_OF);
          }

          // 2. Request값으로 tenant_precontract_check 테이블을 완성한다 : dto -> vo
          TenantPreContractCheckVO preContractCheck = TenantPreContractCheckVO.toVO(request);
          tenantMapper.insertPreContractCheck(preContractCheck);

          // 3. tenant_precontract_check의 FK를 넣고 전세 / 월세 테이블에 값을 넣는다. dto -> vo
          TenantPreContractDTO preContractRequest;

          if (rentType == RentType.JEONSE) {
              request.setTenantPreCheckId(preContractCheck.getTenantPrecheckId());
              TenantJeonseInfoVO jeonseInfo = TenantJeonseInfoVO.toVO(request);
              tenantMapper.insertJeonseInfo(jeonseInfo);
              preContractRequest =
                      TenantPreContractDTO.toDTO(
                              preContractCheck,
                              jeonseInfo.getJeonseLoanPlan(),
                              jeonseInfo.getJeonseInsurancePlan());
          } else if (rentType == RentType.WOLSE) {
              request.setTenantPreCheckId(preContractCheck.getTenantPrecheckId());
              TenantWolseInfoVO wolseInfo = TenantWolseInfoVO.toVO(request);
              tenantMapper.insertWolseInfo(wolseInfo);
              preContractRequest =
                      TenantPreContractDTO.toDTO(
                              preContractCheck,
                              wolseInfo.getWolseLoanPlan(),
                              wolseInfo.getWolseInsurancePlan());
          } else {
              throw new BusinessException(PreContractErrorCode.TENANT_RESPONSE);
          }

          // 4. 그 값을 반환값으로 반환한다. vo -> dto
          return preContractRequest;
      }
}
