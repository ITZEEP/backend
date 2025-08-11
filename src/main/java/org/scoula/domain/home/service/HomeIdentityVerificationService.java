package org.scoula.domain.home.service;

import org.scoula.domain.home.dto.HomeIdentityVerificationDTO;

public interface HomeIdentityVerificationService {

      /**
       * 매물 본인 인증 처리
       *
       * @param homeId 매물 ID
       * @param userId 사용자 ID
       * @param dto 본인 인증 정보
       * @return 성공 시 null
       * @throws RuntimeException 인증 실패 시
       */
      Void verifyAndSaveIdentity(Long homeId, Long userId, HomeIdentityVerificationDTO dto);

      /**
       * 매물 본인 인증 정보 조회
       *
       * @param homeId 매물 ID
       * @return 마스킹된 본인 인증 정보
       */
      HomeIdentityVerificationDTO getIdentityVerification(Long homeId);
}
