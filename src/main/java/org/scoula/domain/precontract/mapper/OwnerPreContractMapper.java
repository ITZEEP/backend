package org.scoula.domain.precontract.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.domain.precontract.dto.owner.OwnerContractStep1DTO;
import org.scoula.domain.precontract.dto.owner.OwnerContractStep2DTO;
import org.scoula.domain.precontract.dto.owner.OwnerLivingStep1DTO;
import org.scoula.domain.precontract.dto.owner.OwnerLivingStep2JeonseDTO;
import org.scoula.domain.precontract.dto.owner.OwnerLivingStep2WolseDTO;
import org.scoula.domain.precontract.dto.owner.OwnerPreContractDTO;
import org.scoula.domain.precontract.vo.*;

@Mapper
public interface OwnerPreContractMapper {
      // userId 확인
      Optional<Long> selectContractOwnerId(@Param("contractChatId") Long contractChatId);

      // 기본 세팅
      // contract_chat 테이블만 기준으로 owner_id 조회 (precheck가 없을 때 사용)
      Optional<Long> selectOwnerIdFromContractChat(@Param("contractChatId") Long contractChatId);

      // // identity_verification에서 identity_id 가져오기
      Optional<Long> selectIdentityId(@Param("userId") Long userId);

      // // rent_type (전/월세) 조회
      Optional<String> selectRentType(
              @Param("contractChatId") Long contractChatId, @Param("userId") Long userId);

      // // owner_precheck_check에 기본 세팅하기 (나머지는 null)
      int insertOwnerPreContractSet(
              @Param("contractChatId") Long contractChatId,
              @Param("identityId") Long identityId,
              @Param("rentType") String rentType);

      // 계약 조건 설정 - step 1
      // // 조회하기
      Optional<OwnerContractStep1DTO> selectContractSub1(
              @Param("userId") Long userId, @Param("contractChatId") Long contractChatId);

      // // 저장하기
      int updateContractSub1(
          @Param("contractChatId") Long contractChatId,
          @Param("dto") OwnerContractStep1DTO dto
      );

      // 계약 조건 설정 - step 2
      // 조회
      Optional<OwnerContractStep2DTO> selectContractSub2(
              @Param("userId") Long userId, @Param("contractChatId") Long contractChatId);

      List<RestoreCategoryVO> selectRestoreScope(
              @Param("contractChatId") Long contractChatId, @Param("userId") Long userId);

      // 계약 조건 저장 (DTO 사용)
      int updateContractSub2(
              @Param("dto") OwnerContractStep2DTO dto,
              @Param("userId") Long userId,
              @Param("contractChatId") Long contractChatId);

      // restore_scope 삭제
      int deleteRestoreScopes(@Param("ownerPrecheckId") Long ownerPrecheckId);

      // restore_scope 삽입
      int insertRestoreScope(
              @Param("ownerPrecheckId") Long ownerPrecheckId,
              @Param("restoreCategoryId") Long restoreCategoryId);

      // 거주 조건 설정 - step 1
      Optional<OwnerLivingStep1DTO> selectLivingSub1(
              @Param("userId") Long userId, @Param("contractChatId") Long contractChatId);

      int updateLivingSub1(
              @Param("dto") OwnerLivingStep1DTO dto,
              @Param("contractChatId") Long contractChatId,
              @Param("userId") Long userId);

      // 거주 조건 설정 - step 2 (전/월세 기준 분기)
      // // 전세 정보 테이블 입력
      int insertJeonseInfo(@Param("vo") OwnerJeonseInfoVO jeonseInfo);

      // // 월세 정보 테이블 입력
      int insertWolseInfo(@Param("vo") OwnerWolseInfoVO wolseInfo);

      // === 전세 조건 저장 ===
      int updateLivingJeonse(
              @Param("dto") OwnerLivingStep2JeonseDTO dto,
              @Param("contractChatId") Long contractChatId,
              @Param("userId") Long userId);

      // === 월세 조건 저장 ===
      int updateLivingWolse(
              @Param("dto") OwnerLivingStep2WolseDTO dto,
              @Param("contractChatId") Long contractChatId,
              @Param("userId") Long userId);

      // === 전세 조건 조회 ===
      Optional<OwnerLivingStep2JeonseDTO> selectLivingJeonse(
              @Param("contractChatId") Long contractChatId, @Param("userId") Long userId);

      // === 월세 조건 조회 ===
      Optional<OwnerLivingStep2WolseDTO> selectLivingWolse(
              @Param("contractChatId") Long contractChatId, @Param("userId") Long userId);

      // 최종 정보 확인
      Optional<OwnerPreContractDTO> selectSummary(@Param("contractChatId") Long contractChatId);
}
