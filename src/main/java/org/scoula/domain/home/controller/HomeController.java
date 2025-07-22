package org.scoula.domain.home.controller;

import org.scoula.domain.home.dto.HealthResponse;
import org.scoula.domain.home.dto.WelcomeResponse;
import org.scoula.global.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/** 홈 컨트롤러 인터페이스 - API 서비스의 기본 엔드포인트와 건강 상태 확인 기능을 정의합니다 */
@Api(tags = "홈 API", description = "기본 엔드포인트 및 헬스체크")
public interface HomeController {

      @ApiOperation(value = "환영 메시지", notes = "API 서비스의 기본 정보를 반환합니다")
      ResponseEntity<ApiResponse<WelcomeResponse>> home();

      @ApiOperation(value = "헬스체크", notes = "서비스의 건강 상태를 확인합니다")
      ResponseEntity<ApiResponse<HealthResponse>> health();

      @ApiOperation(value = "테스트 예외", notes = "예외 처리를 테스트하기 위한 엔드포인트입니다")
      ResponseEntity<ApiResponse<String>> testException();
}
