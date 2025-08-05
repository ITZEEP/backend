package org.scoula.global.oauth2.controller;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.scoula.global.common.dto.ApiResponse;
import org.scoula.global.oauth2.dto.OAuth2TokenResponse;
import org.scoula.global.oauth2.dto.OAuth2UserResponse;
import org.scoula.global.oauth2.service.OAuth2ManualServiceInterface;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/** OAuth2 Callback 및 수동 처리 컨트롤러 */
@RestController
@RequestMapping("/oauth2")
@RequiredArgsConstructor
@Log4j2
@Api(tags = "OAuth2 수동 처리", description = "OAuth2 수동 인증 처리 API")
public class OAuth2CallbackController {

      private final OAuth2ManualServiceInterface oauth2ManualService;

      @Value(
              "${spring.security.oauth2.client.registration.kakao.redirect-uri:http://localhost:5173/oauth/callback}")
      private String frontendRedirectUrl;

      @GetMapping("/callback/{registrationId}")
      @ApiOperation(value = "OAuth2 Callback 처리", notes = "OAuth2 제공자로부터 callback을 받아 프론트엔드로 리다이렉트")
      public void callback(
              @ApiParam(value = "OAuth2 제공자 ID", required = true) @PathVariable String registrationId,
              @ApiParam(value = "Authorization Code") @RequestParam(required = false) String code,
              @ApiParam(value = "오류 코드") @RequestParam(required = false) String error,
              @ApiParam(value = "State 값") @RequestParam(required = false) String state,
              HttpServletRequest request,
              HttpServletResponse response)
              throws Exception {

          log.info(
                  "OAuth2 callback 받음: registrationId={}, code={}, error={}",
                  registrationId,
                  code != null ? "present" : "null",
                  error);

          if (error != null) {
              log.error("OAuth2 인증 실패: {}", error);
              response.sendRedirect(frontendRedirectUrl + "?error=" + error);
              return;
          }

          if (code == null) {
              log.error("Authorization code가 없습니다");
              response.sendRedirect(frontendRedirectUrl + "?error=no_code");
              return;
          }

          // 프론트엔드로 리다이렉트
          log.info("OAuth2 인증 성공, code: {}", code);
          response.sendRedirect(frontendRedirectUrl + "?code=" + code + "&state=" + state);
      }

      @PostMapping("/token/exchange")
      @ApiOperation(
              value = "Authorization Code로 Access Token 교환",
              notes = "Authorization code를 access token으로 교환")
      public ResponseEntity<ApiResponse<OAuth2TokenResponse>> exchangeToken(
              @ApiParam(value = "Authorization Code", required = true) @RequestParam String code) {

          try {
              OAuth2TokenResponse tokenResponse = oauth2ManualService.exchangeCodeForToken(code);
              return ResponseEntity.ok(ApiResponse.success(tokenResponse, "토큰 교환 성공"));
          } catch (Exception e) {
              log.error("토큰 교환 실패", e);
              return ResponseEntity.badRequest()
                      .body(
                              ApiResponse.error(
                                      "TOKEN_EXCHANGE_FAILED", "토큰 교환에 실패했습니다: " + e.getMessage()));
          }
      }

      @PostMapping("/user/info")
      @ApiOperation(value = "Access Token으로 사용자 정보 조회", notes = "Access token을 사용해 사용자 정보를 조회")
      public ResponseEntity<ApiResponse<OAuth2UserResponse>> getUserInfo(
              @ApiParam(value = "Access Token", required = true) @RequestParam String accessToken) {

          try {
              OAuth2UserResponse userResponse = oauth2ManualService.getUserInfo(accessToken);
              return ResponseEntity.ok(ApiResponse.success(userResponse, "사용자 정보 조회 성공"));
          } catch (Exception e) {
              log.error("사용자 정보 조회 실패", e);
              return ResponseEntity.badRequest()
                      .body(
                              ApiResponse.error(
                                      "USER_INFO_FAILED", "사용자 정보 조회에 실패했습니다: " + e.getMessage()));
          }
      }

      @PostMapping("/login/complete")
      @ApiOperation(value = "OAuth2 완전 로그인 처리", notes = "Authorization code부터 JWT 토큰 생성까지 전체 처리")
      public ResponseEntity<ApiResponse<Map<String, String>>> completeLogin(
              @ApiParam(value = "Authorization Code", required = true) @RequestParam String code,
              @ApiParam(value = "Redirect URI", required = false) @RequestParam(required = false)
                      String redirectUri) {

          try {
              Map<String, String> loginResult =
                      oauth2ManualService.processOAuth2Login(code, redirectUri);
              return ResponseEntity.ok(ApiResponse.success(loginResult, "OAuth2 로그인 완료"));
          } catch (Exception e) {
              log.error("OAuth2 로그인 처리 실패", e);
              return ResponseEntity.badRequest()
                      .body(
                              ApiResponse.error(
                                      "LOGIN_FAILED", "OAuth2 로그인에 실패했습니다: " + e.getMessage()));
          }
      }

      @PostMapping("/token/refresh")
      @ApiOperation(value = "Access Token 새로고침", notes = "Refresh token을 사용해 새로운 access token 발급")
      public ResponseEntity<ApiResponse<OAuth2TokenResponse>> refreshToken(
              @ApiParam(value = "Refresh Token", required = true) @RequestParam String refreshToken) {

          try {
              OAuth2TokenResponse tokenResponse =
                      oauth2ManualService.refreshAccessToken(refreshToken);
              return ResponseEntity.ok(ApiResponse.success(tokenResponse, "토큰 새로고침 성공"));
          } catch (Exception e) {
              log.error("토큰 새로고침 실패", e);
              return ResponseEntity.badRequest()
                      .body(
                              ApiResponse.error(
                                      "TOKEN_REFRESH_FAILED", "토큰 새로고침에 실패했습니다: " + e.getMessage()));
          }
      }
}
