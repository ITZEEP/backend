package org.scoula.domain.precontract.service;

import org.scoula.domain.precontract.vo.IdentityVerificationInfoVO;

/** 본인 인증 정보 관리 서비스 인터페이스 암호화된 민감 정보의 저장 및 조회를 담당합니다. */
public interface IdentityVerificationService {

      /**
       * 본인 인증 정보를 조회하고 복호화합니다.
       *
       * @param contractChatId 계약 채팅 ID
       * @param userId 사용자 ID
       * @return 복호화된 본인 인증 정보
       */
      IdentityVerificationInfoVO getDecryptedVerificationInfo(Long contractChatId, Long userId);

      /**
       * 계약서 생성용으로 부분적으로 마스킹된 정보를 조회합니다.
       *
       * @param contractChatId 계약 채팅 ID
       * @param userId 사용자 ID
       * @return 마스킹된 본인 인증 정보
       */
      IdentityVerificationInfoVO getMaskedVerificationInfo(Long contractChatId, Long userId);
}
