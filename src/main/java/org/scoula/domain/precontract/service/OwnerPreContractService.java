package org.scoula.domain.precontract.service;

import org.scoula.domain.precontract.document.ContractDocumentMongoDocument;
import org.scoula.domain.precontract.dto.ai.ContractParseResponseDto;
import org.scoula.domain.precontract.dto.owner.ContractDocumentDTO;
import org.scoula.domain.precontract.dto.owner.OwnerContractStep1DTO;
import org.scoula.domain.precontract.dto.owner.OwnerContractStep2DTO;
import org.scoula.domain.precontract.dto.owner.OwnerInitRespDTO;
import org.scoula.domain.precontract.dto.owner.OwnerLivingStep1DTO;
import org.scoula.domain.precontract.dto.owner.OwnerPreContractDTO;
import org.springframework.web.multipart.MultipartFile;

/** 임대인 사전 계약 정보 설정 서비스 인터페이스 */
public interface OwnerPreContractService {

      /**
       * 계약 채팅이 생성될 때 호출되어, 임대인 사전 조사 기본 정보를 생성합니다.
       *
       * @param contractChatId 계약 채팅 ID
       * @param ownerId 유저 아이디
       * @return 초기화된 사전 정보 ID 등 응답 DTO
       */
      OwnerInitRespDTO saveOwnerInfo(Long contractChatId, Long ownerId);

      // ===== 계약 정보 설정 (contract) =====

      /**
       * 계약 정보 설정 step1 데이터를 저장합니다.
       *
       * @param contractChatId 계약 채팅 ID
       * @param userId 사용자 ID
       * @param contractStep1DTO 계약 조건 입력 DTO
       */
      Void updateOwnerContractStep1(
              Long contractChatId, Long userId, OwnerContractStep1DTO contractStep1DTO);

      /**
       * 계약 정보 설정 step1 데이터를 조회합니다.
       *
       * @param contractChatId 계약 채팅 ID
       * @param userId 사용자 ID
       * @return 저장된 계약 조건 step1 DTO
       */
      OwnerContractStep1DTO selectOwnerContractStep1(Long contractChatId, Long userId);

      /**
       * 계약 정보 설정 step2 데이터를 저장합니다.
       *
       * @param contractChatId 계약 채팅 ID
       * @param userId 사용자 ID
       * @param contractStep2DTO 계약 조건 입력 DTO
       */
      Void updateOwnerContractStep2(
              Long contractChatId, Long userId, OwnerContractStep2DTO contractStep2DTO);

      /**
       * 계약 정보 설정 step2 데이터를 조회합니다.
       *
       * @param contractChatId 계약 채팅 ID
       * @param userId 사용자 ID
       * @return 저장된 계약 조건 step2 DTO
       */
      OwnerContractStep2DTO selectOwnerContractStep2(Long contractChatId, Long userId);

      // ===== 거주 정보 설정 (living) =====

      /**
       * 거주 정보 설정 step1 데이터를 저장합니다.
       *
       * @param contractChatId 계약 채팅 ID
       * @param userId 사용자 ID
       * @param contractStep1DTO 거주 조건 입력 DTO
       */
      Void updateOwnerLivingStep1(
              Long contractChatId, Long userId, OwnerLivingStep1DTO contractStep1DTO);

      /**
       * 거주 정보 설정 step1 데이터를 조회합니다.
       *
       * @param contractChatId 계약 채팅 ID
       * @param userId 사용자 ID
       * @return 저장된 거주 조건 step1 DTO
       */
      OwnerLivingStep1DTO selectOwnerLivingStep1(Long contractChatId, Long userId);

      // ===== 거주 정보 설정 step2 전세/월세 분기 저장 메서드 =====

      // 계약서 특약 OCR 분석
      ContractParseResponseDto analyzeContractDocument(MultipartFile file);

      // 계약서 특약 문서 저장
      void saveContractDocument(Long contractChatId, Long userId, ContractDocumentDTO dto);

      // 계약서 특약 문서 조회
      ContractDocumentMongoDocument getContractDocument(Long contractChatId, Long userId);

      // ===== 최종 정보 통합 =====

      /**
       * 사전 조사한 모든 계약 정보를 통합하여 조회합니다.
       *
       * @param contractChatId 계약 채팅 ID
       * @param userId 사용자 ID
       * @return 통합된 계약 정보 DTO
       */
      OwnerPreContractDTO selectOwnerPreContract(Long contractChatId, Long userId);

      /**
       * 임대인 계약 전 최종본 mongoDB에 저장
       *
       * @param contractChatId 채팅방 아이디
       * @param userId 유저 아이디
       */
      Void saveMongoDB(Long contractChatId, Long userId);
}
