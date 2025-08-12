package org.scoula.domain.contract.service;

import org.scoula.domain.contract.dto.*;

public interface ContractService {

      /**
       * step0. 임차인이 임대인을 기다릴때
       *
       * @param contractChatId 채팅방 아이디
       * @param userId 유저 아이디
       */
      Void standByContract(Long contractChatId, Long userId);

      /**
       * step1 (init) 계약서에 필요한 항목들을 가져와서 몽고 DB에 계약서 만들기
       *
       * @param contractChatId 채팅방 아이디
       * @param userId 유저 아이디
       */
      Void saveContractMongo(Long contractChatId, Long userId);

      /**
       * step1 start 계약서 가져오기
       *
       * @param contractChatId 채팅방 아이디
       * @param userId 유저 아이디
       * @return 계약서 내용을 보내기
       */
      ContractDTO getContract(Long contractChatId, Long userId);

      /**
       * step1
       *
       * @param contractChatId 채팅방 아이디
       * @param userId 유저 아이디
       * @return 계약서 내용을 보내기
       */
      Void getContractNext(Long contractChatId, Long userId);

      /**
       * step1 finish
       *
       * @param contractChatId 채팅방 아이디
       * @param userId 유저 아이디
       * @return 계약서 내용을 보내기
       */
      Boolean nextStep(Long contractChatId, Long userId, NextStepDTO dto);

      /**
       * step2 start 금액을 조율하기 위해 금액을 조회
       *
       * @param contractChatId 채팅방 아이디
       * @param userId 유저 아이디
       */
      PaymentDTO getDepositPrice(Long contractChatId, Long userId);

      /**
       * step2 금액을 레디스에 저장한다.
       *
       * @param contractChatId 채팅방 아이디
       * @param userId 유저 아이디
       * @param dto 변경된 금액값
       */
      Void saveDepositPrice(Long contractChatId, Long userId, PaymentDTO dto);

      /**
       * step2 금액을 레디스에서 삭제한다.
       *
       * @param contractChatId 채팅방 아이디
       * @param userId 유저 아이디
       */
      Void deleteDepositPrice(Long contractChatId, Long userId);

      /**
       * step2 finish 금액을 레디스에서 삭제하고, DB에 저장한다.
       *
       * @param contractChatId 채팅방 아이디
       * @param userId 유저 아이디
       */
      Void updateDepositPrice(Long contractChatId, Long userId);

      /**
       * step4 (init) 계약서를 AI로 보내고, 적법성 받기
       *
       * @param contractChatId 채팅방 아이디
       * @param userId 유저 아이디
       * @return AI가 계약서를 보고 주는 적법성을 리턴값으로 보내기
       */
      LegalityDTO getLegality(Long contractChatId, Long userId);

      /**
       * step4 start 특약을 개약 테이블에 저장하기
       *
       * @param contractChatId 채팅방 아이디
       * @param userId 유저 아이디
       */
      Void saveSpecialContract(Long contractChatId, Long userId);

      /**
       * step4 적법성 검사 후 수정된 특약으로 변경
       *
       * @param contractChatId 채팅방 아이디
       * @param userId 유저 아이디 @Param dto 변경된 특약
       */
      Void updateSpecialContract(Long contractChatId, Long userId, SpecialContractUpdateDTO dto);

      /**
       * step4 finish 적법성 검사 후 다음단계로 넘어가기
       *
       * @param contractChatId 채팅방 아이디
       * @param userId 유저 아이디 @Parma step 계약서 단계
       */
      Void sendStep4(Long contractChatId, Long userId);
}
