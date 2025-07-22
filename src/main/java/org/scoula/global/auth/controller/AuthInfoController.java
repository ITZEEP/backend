package org.scoula.global.auth.controller;

import org.scoula.global.auth.dto.AuthenticatedUserInfo;
import org.scoula.global.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 인증 정보 컨트롤러 인터페이스
 *
 * <p>로그인한 사용자의 인증 정보를 제공하는 API 엔드포인트를 정의합니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@Api(tags = "인증 정보", description = "로그인한 사용자의 인증 정보 조회 API")
public interface AuthInfoController {

      @ApiOperation(value = "현재 로그인한 사용자 정보 조회", notes = "JWT 토큰을 통해 인증된 사용자의 정보를 반환합니다.")
      ResponseEntity<ApiResponse<AuthenticatedUserInfo>> getCurrentUser();
}
