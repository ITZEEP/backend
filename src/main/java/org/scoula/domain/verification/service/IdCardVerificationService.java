package org.scoula.domain.verification.service;

import org.scoula.domain.verification.dto.request.IdCardVerificationRequest;
import org.scoula.domain.verification.dto.response.IdCardVerificationResponse;

/**
 * 주민등록증 진위 확인 서비스 인터페이스
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
public interface IdCardVerificationService {

      /**
       * 주민등록증 진위 여부를 확인합니다.
       *
       * @param request 검증 요청 정보 (이름, 주민번호, 발급일자)
       * @return 검증 성공 시 true, 실패 시 예외 발생
       * @throws org.scoula.domain.verification.exception.VerificationException 검증 실패 시
       */
      boolean verifyIdCard(IdCardVerificationRequest request);

      /**
       * 주민등록증 진위 확인 상세 결과를 반환합니다.
       *
       * @param request 검증 요청 정보 (이름, 주민번호, 발급일자)
       * @return 검증 응답 정보
       * @throws org.scoula.domain.verification.exception.VerificationException API 호출 실패 시
       */
      IdCardVerificationResponse getVerificationDetail(IdCardVerificationRequest request);
}
