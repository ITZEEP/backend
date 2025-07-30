package org.scoula.domain.precontract.service;

import org.scoula.domain.precontract.dto.tenant.*;

public interface PreContractService {

      // =============== 사기 위험도 확인 & 기본 세팅 ==================

      /**
       * 계약서 작성 전, 사기위험도 조사를 했는지 확인하기
       *
       * @param contractChatId 채팅방 아이디
       * @param userId 유저 아이디
       * @return 사기위헙도 조사 여부를 boolean 값으로 보내기
       */
      Boolean getCheckRisk(Long contractChatId, Long userId);

      // =============== 본인인증 하기 ==================

      /**
       * 계약채팅이 만들어지면 그곳에서 호출되어 '임차인 계약 전 사전 정보' 컬럼이 만들어진다. -> 임차인 사전 정보 기본 세팅
       *
       * @param contractChatId 채팅방 아이디
       * @param buyerId 유저 아이디
       */
      TenantInitRespDTO saveTenantInfo(Long contractChatId, Long buyerId);

      // =============== step 1 ==================

      /**
       * 계약채팅이 만들어지면 그곳에서 호출되어 '임차인 계약 전 사전 정보' 컬럼이 만들어진다.
       *
       * @param contractChatId 채팅방 아이디
       * @param userId 유저 아이디
       * @param step1DTO 계약전 step1 입력사항
       */
      Void updateTenantStep1(Long contractChatId, Long userId, TenantStep1DTO step1DTO);

      /**
       * 임차인 계약 전 step1 조회
       *
       * @param contractChatId 채팅방 아이디
       * @param userId 유저 아이디
       * @return step2 컬럼 조회
       */
      TenantStep1DTO selectTenantStep1(Long contractChatId, Long userId);

      // =============== step 2 ==================

      /**
       * 계약채팅이 만들어지면 그곳에서 호출되어 '임차인 계약 전 사전 정보' 컬럼이 만들어진다.
       *
       * @param contractChatId 채팅방 아이디
       * @param userId 유저 아이디
       * @param step2DTO 계약전 step2 입력사항
       */
      Void updateTenantStep2(Long contractChatId, Long userId, TenantStep2DTO step2DTO);

      /**
       * 임차인 계약 전 step2 조회
       *
       * @param contractChatId 채팅방 아이디
       * @param userId 유저 아이디
       * @return step2 컬럼 조회
       */
      TenantStep2DTO selectTenantStep2(Long contractChatId, Long userId);

      // =============== step 3 ==================

      /**
       * 계약채팅이 만들어지면 그곳에서 호출되어 '임차인 계약 전 사전 정보' 컬럼이 만들어진다.
       *
       * @param contractChatId 채팅방 아이디
       * @param userId 유저 아이디
       * @param step3DTO 계약전 step3 입력사항
       */
      Void updateTenantStep3(Long contractChatId, Long userId, TenantStep3DTO step3DTO);

      /**
       * 임차인 계약 전 step3 조회
       *
       * @param contractChatId 채팅방 아이디
       * @param userId 유저 아이디
       * @return step3 컬럼 조회
       */
      TenantStep3DTO selectTenantStep3(Long contractChatId, Long userId);

      // =============== 최종 ==================

      /**
       * 임차인 계약 전 최종본 조회
       *
       * @param contractChatId 채팅방 아이디
       * @param userId 유저 아이디
       * @return 계약전 최종본 조회
       */
      TenantPreContractDTO selectTenantPreCon(Long contractChatId, Long userId);

      /**
       * 임차인 계약 전 최종본 mongoDB에 저장
       *
       * @param contractChatId 채팅방 아이디
       * @param userId 유저 아이디
       */
      Void saveMongoDB(Long contractChatId, Long userId);
}
