package org.scoula.domain.contract.service;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.scoula.domain.contract.dto.*;
import org.springframework.web.multipart.MultipartFile;

public interface ContractService {

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
       * step4 적법성 검사 후 수정된 특약으로 변경
       *
       * @param contractChatId 채팅방 아이디
       * @param userId 유저 아이디 @Param dto 변경된 특약
       */
      Void updateSpecialContract(Long contractChatId, Long userId, SpecialContractUpdateDTO dto);

      /**
       * step4 (init) 계약서를 AI로 보내고, 적법성 받기
       *
       * @param contractChatId 채팅방 아이디
       * @param userId 유저 아이디
       * @return AI가 계약서를 보고 주는 적법성을 리턴값으로 보내기
       */
      String deleteOwnerLegality(Long contractChatId, Long userId);

      /**
       * step4 (init) 계약서를 AI로 보내고, 적법성 받기
       *
       * @param contractChatId 채팅방 아이디
       * @param userId 유저 아이디
       * @return AI가 계약서를 보고 주는 적법성을 리턴값으로 보내기
       */
      Void updateOwnerLegality(Long contractChatId, Long userId, UpdateLegalityDTO dto);

      /**
       * step4 (init) 계약서를 AI로 보내고, 적법성 받기
       *
       * @param contractChatId 채팅방 아이디
       * @param userId 유저 아이디
       * @return AI가 계약서를 보고 주는 적법성을 리턴값으로 보내기
       */
      Void updateBuyerLegality(Long contractChatId, Long userId, SpecialContractUpdateDTO dto);

      /**
       * step4 (init) 계약서를 AI로 보내고, 적법성 받기
       *
       * @param contractChatId 채팅방 아이디
       * @param userId 유저 아이디
       * @return AI가 계약서를 보고 주는 적법성을 리턴값으로 보내기
       */
      String rejectBuyerLegality(Long contractChatId, Long userId);

      /**
       * step4 finish 적법성 검사 후 다음단계로 넘어가기
       *
       * @param contractChatId 채팅방 아이디
       * @param userId 유저 아이디 @Parma step 계약서 단계
       */
      Void sendStep4(Long contractChatId, Long userId);

      // ============================================
      /**
       * 계약서 내보내기 시작 - AI 서버에서 초기 PDF 생성
       *
       * @param contractChatId 채팅방 아이디
       * @param userId 유저 아이디
       * @return PDF 바이트 배열
       */
      byte[] startContractExport(Long contractChatId, Long userId);

      /**
       * 서명이 포함된 계약서 PDF 생성
       *
       * @param contractChatId 계약 채팅 ID
       * @param userId 사용자 ID
       * @param ownerSignatures 임대인 서명 목록
       * @param buyerSignatures 임차인 서명 목록
       * @param ownerHasTaxArrears 임대인 세금체납 여부
       * @param ownerHasPriorFixedDate 임대인 선순위확정일자 여부
       * @param ownerMediationAgree 임대인 조정 동의 여부
       * @param buyerMediationAgree 임차인 조정 동의 여부
       * @return 서명이 포함된 PDF 바이트 배열
       */
      byte[] generateContractWithSignatures(
              Long contractChatId,
              Long userId,
              java.util.List<String> ownerSignatures,
              java.util.List<String> buyerSignatures,
              boolean ownerHasTaxArrears,
              boolean ownerHasPriorFixedDate,
              boolean ownerMediationAgree,
              boolean buyerMediationAgree);

      /**
       * 최종 계약서 작성하기 PDF -> AI
       *
       * @param contractChatId 채팅방 아이디
       * @param userId 유저 아이디
       */
      MultipartFile finalContractPDF(Long contractChatId, Long userId);

      /**
       * 전자서명 파일 암호화 후 S3에 저장 (암호화 형식이 다름)
       *
       * @param contractChatId 채팅방 아이디
       * @param userId 유저 아이디
       */
      Boolean saveSignature(
              Long contractChatId,
              Long userId,
              SaveSignatureDTO signatureDTO,
              List<MultipartFile> imgFiles)
              throws Exception;

      /**
       * 최종계약서 S3에 저장
       *
       * @param contractChatId 채팅방 아이디
       * @param userId 유저 아이디
       */
      byte[] saveFinalContract(Long contractChatId, Long userId, ContractPasswordDTO dto);

      /**
       * 최종 계약서를 불러와서 보내주기
       *
       * @param contractChatId 채팅방 아이디
       * @param userId 유저 아이디
       */
      byte[] selectContractPDF(Long contractChatId, Long userId, ContractPasswordDTO dto);

      //      /**
      //       * 계약서 PDF 파일 암호화 후 S3에 저장 (암호화 형식이 다름)
      //       *
      //       * @param contractChatId 채팅방 아이디
      //       * @param userId 유저 아이디 @Parma step 계약서 단계 @Param dto 실제 계약서에 있는 내역들
      //       */
      //      Void saveContractPDF(Long contractChatId, Long userId, FinalContractDTO dto);

      //      /**
      //       * 전자서명 다운로드 (복호화하기)
      //       *
      //       * @param contractChatId 채팅방 아이디
      //       * @param userId 유저 아이디 @Parma step 계약서 단계 @Param dto 실제 계약서에 있는 내역들
      //       */
      //      Void selectSignaturePDF(Long contractChatId, Long userId, HttpServletResponse response);

      /**
       * 계약서 PDF 파일 다운로드/인쇄하기 -> 프론트에 보내기
       *
       * @param contractChatId 채팅방 아이디
       * @param userId 유저 아이디 @Parma step 계약서 단계
       */
      Void selectContractPDF(
              Long contractChatId, Long userId, HttpServletResponse response, FindContractDTO dto)
              throws Exception;

      /**
       * 계약서 PDF를 이메일로 전송
       *
       * @param contractChatId 채팅방 아이디
       * @param userId 유저 아이디 @Parma step 계약서 단계
       */
      Void sendContractPDF(Long contractChatId, Long userId, FindContractDTO dto) throws Exception;

      /**
       * 사용자의 생년월일 가져오기
       *
       * @param contractChatId 계약 채팅 ID
       * @param userId 사용자 ID
       * @param userRole 사용자 역할 (owner/buyer)
       * @return 생년월일 (YYMMDD 형식)
       */
      String getUserBirthDate(Long contractChatId, Long userId, String userRole);

      /**
       * 최종 계약서를 데이터베이스에 저장
       *
       * @param contractChatId 계약 채팅 ID
       * @param s3Url S3 URL (전체 URL)
       * @param pdfHash PDF 해시값
       */
      void saveFinalContractToDatabase(Long contractChatId, String s3Url, String pdfHash);

      /**
       * 기존 계약서 PDF 가져오기 (fallback용)
       *
       * @param contractChatId 계약 채팅 ID
       * @return PDF 바이트 배열
       */
      byte[] getExistingContractPdf(Long contractChatId);
}
