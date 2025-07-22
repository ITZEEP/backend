package org.scoula.global.oauth2.controller;

import javax.servlet.http.HttpServletResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * OAuth2 인증 엔드포인트 컨트롤러 인터페이스
 *
 * <p>OAuth2 제공자로의 인증 리다이렉션을 처리합니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@Api(tags = "OAuth2 인증", description = "OAuth2 인증 시작 API")
public interface OAuth2AuthorizationControllerInterface {

      @ApiOperation(value = "OAuth2 인증 시작", notes = "지정된 OAuth2 제공자로 사용자를 리다이렉트합니다.")
      void authorize(
              @ApiParam(value = "OAuth2 제공자 ID (예: kakao)", required = true) String registrationId,
              @ApiParam(hidden = true) HttpServletResponse response)
              throws Exception;
}
