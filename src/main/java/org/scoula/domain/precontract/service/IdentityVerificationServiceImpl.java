package org.scoula.domain.precontract.service;

import org.scoula.domain.precontract.exception.PreContractErrorCode;
import org.scoula.domain.precontract.mapper.OwnerPreContractMapper;
import org.scoula.domain.precontract.vo.IdentityVerificationInfoVO;
import org.scoula.global.common.exception.BusinessException;
import org.scoula.global.common.util.AesCryptoUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/** 본인 인증 정보 관리 서비스 구현체 암호화된 민감 정보의 저장 및 조회를 담당합니다. */
@Service
@RequiredArgsConstructor
@Log4j2
public class IdentityVerificationServiceImpl implements IdentityVerificationService {

      private final OwnerPreContractMapper ownerMapper;
      private final AesCryptoUtil aesCryptoUtil;

      @Override
      @Transactional(readOnly = true)
      public IdentityVerificationInfoVO getDecryptedVerificationInfo(
              Long contractChatId, Long userId) {
          // 1. DB에서 암호화된 데이터 조회
          IdentityVerificationInfoVO encryptedInfo =
                  ownerMapper
                          .selectIdentityVerificationInfo(contractChatId, userId)
                          .orElseThrow(
                                  () -> new BusinessException(PreContractErrorCode.TENANT_SELECT));

          // 2. 복호화 처리
          return decryptVerificationInfo(encryptedInfo);
      }

      @Override
      @Transactional(readOnly = true)
      public IdentityVerificationInfoVO getMaskedVerificationInfo(Long contractChatId, Long userId) {
          // 1. 복호화된 정보 조회
          IdentityVerificationInfoVO decryptedInfo =
                  getDecryptedVerificationInfo(contractChatId, userId);

          // 2. 민감 정보 마스킹 처리
          return maskSensitiveInfo(decryptedInfo);
      }

      /**
       * 암호화된 본인 인증 정보를 복호화합니다.
       *
       * @param encryptedInfo 암호화된 정보
       * @return 복호화된 정보
       */
      private IdentityVerificationInfoVO decryptVerificationInfo(
              IdentityVerificationInfoVO encryptedInfo) {
          if (encryptedInfo == null) {
              return null;
          }

          try {
              // 복호화가 필요한 필드만 처리
              IdentityVerificationInfoVO decryptedInfo =
                      IdentityVerificationInfoVO.builder()
                              .identityId(encryptedInfo.getIdentityId())
                              .userId(encryptedInfo.getUserId())
                              .contractId(encryptedInfo.getContractId())
                              .name(encryptedInfo.getName()) // 이름은 평문
                              .ssnFront(encryptedInfo.getSsnFront()) // 앞자리는 평문
                              .ssnBack(decryptSafely(encryptedInfo.getSsnBack())) // 뒷자리 복호화
                              .addr1(encryptedInfo.getAddr1()) // 기본 주소는 평문
                              .addr2(decryptSafely(encryptedInfo.getAddr2())) // 상세 주소 복호화
                              .phoneNumber(decryptSafely(encryptedInfo.getPhoneNumber())) // 전화번호 복호화
                              .identityVerifiedAt(encryptedInfo.getIdentityVerifiedAt())
                              .contractStep(encryptedInfo.getContractStep())
                              .build();

              log.debug("본인 인증 정보 복호화 완료 - userId: {}", encryptedInfo.getUserId());
              return decryptedInfo;

          } catch (Exception e) {
              log.error("본인 인증 정보 복호화 실패 - userId: {}", encryptedInfo.getUserId(), e);
              throw new BusinessException(
                      PreContractErrorCode.TENANT_SELECT, "본인 인증 정보 복호화에 실패했습니다.");
          }
      }

      /**
       * 안전하게 복호화를 수행합니다. (null 체크 포함)
       *
       * @param encryptedText 암호화된 텍스트
       * @return 복호화된 텍스트 또는 null
       */
      private String decryptSafely(String encryptedText) {
          if (encryptedText == null || encryptedText.isEmpty()) {
              return null;
          }

          // 암호화된 데이터인지 확인
          if (aesCryptoUtil.isEncrypted(encryptedText)) {
              return aesCryptoUtil.decrypt(encryptedText);
          }

          // 암호화되지 않은 데이터는 그대로 반환 (마이그레이션 고려)
          log.warn("암호화되지 않은 데이터 발견. 마이그레이션이 필요할 수 있습니다.");
          return encryptedText;
      }

      /**
       * 민감 정보를 마스킹 처리합니다.
       *
       * @param info 원본 정보
       * @return 마스킹된 정보
       */
      private IdentityVerificationInfoVO maskSensitiveInfo(IdentityVerificationInfoVO info) {
          if (info == null) {
              return null;
          }

          return IdentityVerificationInfoVO.builder()
                  .identityId(info.getIdentityId())
                  .userId(info.getUserId())
                  .contractId(info.getContractId())
                  .name(info.getName())
                  .ssnFront(info.getSsnFront())
                  .ssnBack(maskSsnBack(info.getSsnBack())) // 뒷자리 마스킹
                  .addr1(info.getAddr1())
                  .addr2(info.getAddr2()) // 상세 주소는 유지
                  .phoneNumber(maskPhoneNumber(info.getPhoneNumber())) // 전화번호 마스킹
                  .identityVerifiedAt(info.getIdentityVerifiedAt())
                  .contractStep(info.getContractStep())
                  .build();
      }

      /**
       * 주민등록번호 뒷자리를 마스킹합니다. 예: 1234567 -> 1******
       *
       * @param ssnBack 원본 주민등록번호 뒷자리
       * @return 마스킹된 주민등록번호 뒷자리
       */
      private String maskSsnBack(String ssnBack) {
          if (ssnBack == null || ssnBack.length() < 2) {
              return ssnBack;
          }

          return ssnBack.charAt(0) + "******";
      }

      /**
       * 전화번호를 마스킹합니다. 예: 010-1234-5678 -> 010-****-5678
       *
       * @param phoneNumber 원본 전화번호
       * @return 마스킹된 전화번호
       */
      private String maskPhoneNumber(String phoneNumber) {
          if (phoneNumber == null || phoneNumber.length() < 8) {
              return phoneNumber;
          }

          // 하이픈 제거
          String cleaned = phoneNumber.replaceAll("[^0-9]", "");

          if (cleaned.length() == 11) { // 010-1234-5678 형식
              return cleaned.substring(0, 3) + "-****-" + cleaned.substring(7);
          } else if (cleaned.length() == 10) { // 010-123-4567 형식
              return cleaned.substring(0, 3) + "-***-" + cleaned.substring(6);
          }

          // 기타 형식은 중간 부분 마스킹
          int maskStart = cleaned.length() / 3;
          int maskEnd = cleaned.length() * 2 / 3;
          StringBuilder masked = new StringBuilder(cleaned.substring(0, maskStart));
          for (int i = maskStart; i < maskEnd; i++) {
              masked.append('*');
          }
          masked.append(cleaned.substring(maskEnd));

          return masked.toString();
      }
}
