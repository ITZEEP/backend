package org.scoula.global.auth.controller;

import org.scoula.global.auth.dto.OAuth2Dto;
import org.scoula.global.common.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.log4j.Log4j2;

@RestController
@RequestMapping("/api/auth")
@Log4j2
public class OAuth2ControllerImpl implements OAuth2Controller {

      @Value("${app.base-url:http://localhost:8080}")
      private String baseUrl;

      @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
      private String kakaoClientId;

      @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
      private String kakaoRedirectUri;

      /** {@inheritDoc} */
      @Override
      @GetMapping("/kakao/login-url")
      public ResponseEntity<ApiResponse<OAuth2Dto.LoginUrlResponse>> getKakaoLoginUrl() {
          try {
              // Direct link to OAuth2AuthorizationController endpoint
              String loginUrl = baseUrl + "/oauth2/authorization/kakao";

              OAuth2Dto.LoginUrlResponse response =
                      OAuth2Dto.LoginUrlResponse.builder()
                              .loginUrl(loginUrl)
                              .method("GET")
                              .description("브라우저에서 해당 URL로 접속하면 카카오 로그인 페이지로 리다이렉트됩니다.")
                              .build();

              log.debug("카카오 로그인 URL 요청: {}", loginUrl);
              return ResponseEntity.ok(ApiResponse.success(response, "카카오 로그인 URL 조회 성공"));
          } catch (Exception e) {
              log.error("카카오 로그인 URL 조회 실패", e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(ApiResponse.error("OAUTH_ERROR", e.getMessage()));
          }
      }
}
