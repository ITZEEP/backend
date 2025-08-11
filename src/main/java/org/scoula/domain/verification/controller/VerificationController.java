package org.scoula.domain.verification.controller;

import javax.validation.Valid;

import org.scoula.domain.verification.dto.request.IdCardVerificationRequest;
import org.scoula.domain.verification.dto.response.IdCardVerificationResponse;
import org.scoula.global.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * 본인인증 관련 컨트롤러 인터페이스
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@Api(tags = {"본인인증 API"})
public interface VerificationController {

      /**
       * 주민등록증 진위 확인 테스트용 API
       *
       * @param request 주민등록증 진위 확인 요청 정보
       * @return 진위 확인 상세 결과
       */
      @ApiOperation(
              value = "주민등록증 진위 확인 테스트",
              notes = "주민등록증 진위 확인을 테스트합니다. 이름, 주민등록번호, 발급일자를 통해 검증하며, 상세 결과를 반환합니다.")
      @PostMapping("/idcard/test")
      ResponseEntity<ApiResponse<IdCardVerificationResponse>> testVerifyIdCard(
              @ApiParam(value = "주민등록증 진위 확인 요청 정보", required = true) @Valid @RequestBody
                      IdCardVerificationRequest request);
}
