package org.scoula.domain.home.controller;

import javax.validation.Valid;

import org.scoula.domain.home.dto.HomeIdentityVerificationDTO;
import org.scoula.global.common.dto.ApiResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Api(tags = "매물 본인 인증 API")
@RequestMapping("/api/home")
public interface HomeIdentityVerificationController {

      @ApiOperation(value = "매물 본인 인증", notes = "매물 소유자의 본인 인증을 처리합니다")
      @PostMapping("/{homeId}/identity-verification")
      ApiResponse<Void> verifyIdentity(
              @ApiParam(value = "매물 ID", required = true) @PathVariable Long homeId,
              Authentication authentication,
              @ApiParam(value = "본인 인증 정보", required = true) @Valid @RequestBody
                      HomeIdentityVerificationDTO dto);

      @ApiOperation(value = "매물 본인 인증 정보 조회", notes = "매물의 본인 인증 정보를 조회합니다")
      @GetMapping("/{homeId}/identity-verification")
      ApiResponse<HomeIdentityVerificationDTO> getIdentityVerification(
              @ApiParam(value = "매물 ID", required = true) @PathVariable Long homeId);
}
