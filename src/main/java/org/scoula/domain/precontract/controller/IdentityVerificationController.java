package org.scoula.domain.precontract.controller;

import org.scoula.domain.precontract.dto.common.IdentityVerificationInfoDTO;
import org.scoula.global.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(tags = "계약 사전 조사 본인인증", description = "임대인/임차인 계약 작성 전 사전 조사")
public interface IdentityVerificationController {
      // 본인 인증
      @ApiOperation(
              value = "계약 사전 조사 본인 인증 완료 처리",
              notes = "임대인/임차인 계약 사전 조사에서 본인 인증 완료 후 DB에 저장합니다.")
      ResponseEntity<ApiResponse<String>> saveVerification(
              @PathVariable Long contractChatId,
              Authentication authentication,
              IdentityVerificationInfoDTO dto);
}
