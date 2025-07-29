package org.scoula.domain.precontract.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.domain.precontract.dto.TenantPreContractDTO;
import org.scoula.domain.precontract.dto.TenantStep1DTO;
import org.scoula.domain.precontract.dto.TenantStep2DTO;
import org.scoula.domain.precontract.dto.TenantStep3DTO;
import org.scoula.domain.precontract.enums.RentType;
import org.scoula.domain.precontract.vo.TenantJeonseInfoVO;
import org.scoula.domain.precontract.vo.TenantPreContractCheckVO;
import org.scoula.domain.precontract.vo.TenantWolseInfoVO;

@Mapper
public interface TenantPreContractMapper {

      // =============== 사기 위험도 확인 & 기본 세팅 ==================

      // risk_check에 맞는 risk_id가 있는지 확인하기
      Long selectRiskId(@Param("contractChatId") Long contractChatId, @Param("userId") Long userid);

      // risk type 가져오기
      String selectRiskType(
              @Param("contractChatId") Long contractChatId, @Param("userId") Long userid);

      // identity_verification에서 identity_id 가져오기
      Long selectIdentityId(@Param("userId") Long userId);

      // tenant_preCheck_check에 기본 세팅 하기 (나머지는 다 Null)
      int insertPreContractSet(
              @Param("contractChatId") Long contractChatId,
              @Param("identityId") Long identityId,
              @Param("riskId") Long riskId,
              @Param("rentType") String rentType,
              @Param("riskType") String riskType);

      String selectRentType(
              @Param("contractChatId") Long contractChatId, @Param("userId") Long userid);

      // =============== step 1 ==================

      // 전세 / 월세 확인하기
      String selectLeaseType(
              @Param("userId") Long userId, @Param("contractChatId") Long contractChatId);

      // 전세 정보 테이블 입력
      int insertJeonseInfo(@Param("vo") TenantJeonseInfoVO jeonseInfo);

      // 월세 정보 테이블 입력
      int insertWolseInfo(@Param("vo") TenantWolseInfoVO wolseInfo);

      // step1 저장(update) 하기
      int updateStep1(
              @Param("vo") TenantPreContractCheckVO vo,
              @Param("rentType") RentType rentType,
              @Param("userId") Long userId,
              @Param("contractChatId") Long contractChatId);

      // 애완동물 가능 여부 조회하기 -> 반환값에 넣어서 다음 페이지 준비하기
      boolean selectIsPet(@Param("userId") Long userId, @Param("contractChatId") Long contractChatId);

      // step1 조회하기
      TenantStep1DTO selectStep1(
              @Param("userId") Long userId, @Param("contractChatId") Long contractChatId);

      // =============== step 2 ==================

      // step2 저장(update) 하기
      int updateStep2(
              @Param("vo") TenantPreContractCheckVO vo,
              @Param("userId") Long userId,
              @Param("contractChatId") Long contractChatId);

      // step2 조회하기
      TenantStep2DTO selectStep2(
              @Param("userId") Long userId, @Param("contractChatId") Long contractChatId);

      // =============== step 3 ==================

      // step3 저장(update)하기
      int updateStep3(
              @Param("vo") TenantPreContractCheckVO vo,
              @Param("userId") Long userId,
              @Param("contractChatId") Long contractChatId);

      // step3 조회하기
      TenantStep3DTO selectStep3(
              @Param("userId") Long userId, @Param("contractChatId") Long contractChatId);

      // =============== 최종 ==================

      // 최종 데이터 조회하기
      TenantPreContractDTO selectPreCon(
              @Param("userId") Long userId, @Param("contractChatId") Long contractChatId);
}
