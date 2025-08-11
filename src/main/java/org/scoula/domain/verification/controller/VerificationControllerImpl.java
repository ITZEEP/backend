package org.scoula.domain.verification.controller;

import javax.validation.Valid;

import org.scoula.domain.verification.dto.request.IdCardVerificationRequest;
import org.scoula.domain.verification.dto.response.IdCardVerificationResponse;
import org.scoula.domain.verification.service.IdCardVerificationService;
import org.scoula.global.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * 본인인증 관련 컨트롤러 구현체
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/verification")
@RequiredArgsConstructor
@Log4j2
public class VerificationControllerImpl implements VerificationController {

      private final IdCardVerificationService idCardVerificationService;

      /**
       * 주민등록증 진위 확인 테스트용 API
       *
       * @param request 주민등록증 진위 확인 요청 정보
       * @return 진위 확인 상세 결과
       */
      @Override
      @PostMapping("/idcard/test")
      public ResponseEntity<ApiResponse<IdCardVerificationResponse>> testVerifyIdCard(
              @Valid @RequestBody IdCardVerificationRequest request) {

          log.info("[TEST] 주민등록증 진위 확인 요청 - request: {}", request.getMaskedInfo());

          IdCardVerificationResponse response =
                  idCardVerificationService.getVerificationDetail(request);
          log.info(
                  "[TEST] 주민등록증 진위 확인 완료 - verificationId: {}, result: {}",
                  response.getData() != null ? response.getData().getIcId() : "null",
                  response.getData() != null ? response.getData().getResult() : "null");

          return ResponseEntity.ok(ApiResponse.success(response, "주민등록증 진위 확인 결과를 조회했습니다."));
      }
}
