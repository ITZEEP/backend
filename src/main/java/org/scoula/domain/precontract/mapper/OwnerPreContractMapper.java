package org.scoula.domain.precontract.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.domain.precontract.dto.owner.OwnerContractStep1DTO;
import org.scoula.domain.precontract.dto.owner.OwnerContractStep2DTO;
import org.scoula.domain.precontract.dto.owner.OwnerLivingStep1DTO;
import org.scoula.domain.precontract.dto.owner.OwnerPreContractDTO;
import org.scoula.domain.precontract.dto.owner.OwnerPreContractMongoDTO;
import org.scoula.domain.precontract.vo.*;

@Mapper
public interface OwnerPreContractMapper {

      // 본인 인증 성공 후 identity_verification 테이블에 내용 주입
      int insertIdentityVerification(
              @Param("contractChatId") Long contractChatId,
              @Param("userId") Long userId,
              @Param("vo") IdentityVerificationInfoVO vo);

      // 본인 인증 정보 조회
      Optional<IdentityVerificationInfoVO> selectIdentityVerificationInfo(
              @Param("contractChatId") Long contractChatId, @Param("userId") Long userId);

      // 본인 인증 정보 업데이트
      int updateIdentityVerification(
              @Param("contractChatId") Long contractChatId,
              @Param("vo") IdentityVerificationInfoVO vo);

      // userId 확인
      Optional<Long> selectContractOwnerId(@Param("contractChatId") Long contractChatId);

      // 기본 세팅
      // contract_chat 테이블만 기준으로 owner_id 조회 (precheck가 없을 때 사용)
      Optional<Long> selectOwnerIdFromContractChat(@Param("contractChatId") Long contractChatId);

      // // identity_verification에서 identity_id 가져오기
      Optional<Long> selectIdentityId(@Param("contractChatId") Long contractChatId);

      // // rent_type (전/월세) 조회
      Optional<String> selectRentType(
              @Param("contractChatId") Long contractChatId, @Param("userId") Long userId);

      // // owner_precheck_check에 기본 세팅하기 (나머지는 null)
      int insertOwnerPreContractSet(
              @Param("contractChatId") Long contractChatId,
              @Param("identityId") Long identityId,
              @Param("rentType") String rentType);

      // // 전세 정보 테이블 입력
      int insertJeonseInfo(@Param("vo") OwnerJeonseInfoVO jeonseInfo);

      // // 월세 정보 테이블 입력
      int insertWolseInfo(@Param("vo") OwnerWolseInfoVO wolseInfo);

      // 계약 조건 설정 - step 1
      // // 조회하기
      Optional<OwnerContractStep1DTO> selectContractSub1(
              @Param("contractChatId") Long contractChatId, @Param("userId") Long userId);

      // // 저장하기
      int updateContractSub1(
              @Param("contractChatId") Long contractChatId,
              @Param("userId") Long userId,
              @Param("dto") OwnerContractStep1DTO dto);

      // 계약 조건 설정 - step 2
      // 조회
      Optional<OwnerContractStep2DTO> selectContractSub2(
              @Param("contractChatId") Long contractChatId, @Param("userId") Long userId);

      List<RestoreCategoryVO> selectRestoreScope(
              @Param("contractChatId") Long contractChatId, @Param("userId") Long userId);

      Long selectRestoreCategoryIdByName(@Param("name") String name);

      // 계약 조건 저장 (DTO 사용)
      int updateContractSub2(
              @Param("dto") OwnerContractStep2DTO dto,
              @Param("contractChatId") Long contractChatId,
              @Param("userId") Long userId);

      // restore_scope UPSERT
      int upsertRestoreScope(
              @Param("ownerPrecheckId") Long ownerPrecheckId,
              @Param("restoreCategoryId") Long restoreCategoryId);

      // 거주 조건 설정 - step 1
      Optional<OwnerLivingStep1DTO> selectLivingSub1(
              @Param("contractChatId") Long contractChatId, @Param("userId") Long userId);

      int updateLivingSub1(
              @Param("dto") OwnerLivingStep1DTO dto,
              @Param("contractChatId") Long contractChatId,
              @Param("userId") Long userId);

      // === 전세 조건 저장 ===
      int updateLivingJeonse(
              @Param("contractChatId") Long contractChatId,
              @Param("userId") Long userId,
              @Param("allowJeonseRightRegistration") Boolean allowJeonseRightRegistration);

      // === 월세 조건 저장 ===
      int updateLivingWolse(
              @Param("contractChatId") Long contractChatId,
              @Param("userId") Long userId,
              @Param("paymentDueDate") Integer paymentDueDate,
              @Param("lateFeeInterestRate") Double lateFeeInterestRate);

      // === 전세 조건 조회 ===
      Optional<OwnerJeonseInfoVO> selectLivingJeonse(
              @Param("contractChatId") Long contractChatId, @Param("userId") Long userId);

      // === 월세 조건 조회 ===
      Optional<OwnerWolseInfoVO> selectLivingWolse(
              @Param("contractChatId") Long contractChatId, @Param("userId") Long userId);

      // 최종 정보 확인
      Optional<OwnerPreContractDTO> selectOwnerPreContractSummary(
              @Param("contractChatId") Long contractChatId, @Param("userId") Long userId);

      Optional<Long> selectOwnerPrecheckId(
              @Param("contractChatId") Long contractChatId, @Param("userId") Long userId);

      // 최종 정보 MongoDB에 저장
      // 몽고 DB에 데이터 넘기기
      OwnerPreContractMongoDTO selectMongo(
              @Param("contractChatId") Long contractChatId, @Param("userId") Long userId);
}
