package org.scoula.global.auth.controller;

import org.scoula.global.auth.dto.OAuth2Dto;
import org.scoula.global.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponses;

@Api(tags = "OAuth2 인증 API", description = "OAuth2 로그인 관련 엔드포인트")
public interface OAuth2Controller {

      @ApiOperation(value = "카카오 로그인 URL 조회", notes = "카카오 OAuth2 로그인을 시작할 수 있는 URL을 반환합니다")
      @ApiResponses({
          @io.swagger.annotations.ApiResponse(code = 200, message = "URL 조회 성공"),
          @io.swagger.annotations.ApiResponse(code = 500, message = "서버 오류")
      })
      ResponseEntity<ApiResponse<OAuth2Dto.LoginUrlResponse>> getKakaoLoginUrl();
}
