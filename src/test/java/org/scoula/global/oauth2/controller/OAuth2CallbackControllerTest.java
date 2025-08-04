package org.scoula.global.oauth2.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scoula.global.oauth2.dto.OAuth2TokenResponse;
import org.scoula.global.oauth2.dto.OAuth2UserResponse;
import org.scoula.global.oauth2.service.OAuth2ManualServiceInterface;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * OAuth2CallbackController 단위 테스트
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2CallbackController 단위 테스트")
class OAuth2CallbackControllerTest {

      @Mock private OAuth2ManualServiceInterface oauth2ManualService;

      @InjectMocks private OAuth2CallbackController oauth2CallbackController;

      private MockMvc mockMvc;

      private static final String FRONTEND_REDIRECT_URL = "http://localhost:3000/oauth/callback";

      @BeforeEach
      void setUp() {
          mockMvc = MockMvcBuilders.standaloneSetup(oauth2CallbackController).build();
          ReflectionTestUtils.setField(
                  oauth2CallbackController, "frontendRedirectUrl", FRONTEND_REDIRECT_URL);
      }

      @Nested
      @DisplayName("OAuth2 Callback 처리")
      class CallbackTest {

          @Test
          @DisplayName("정상적인 callback 처리 - code와 함께 프론트엔드로 리다이렉트")
          void callback_Success() throws Exception {
              // given
              String code = "test-auth-code";
              String state = "test-state";

              // when & then
              mockMvc.perform(get("/oauth2/callback/kakao").param("code", code).param("state", state))
                      .andExpect(status().is3xxRedirection())
                      .andExpect(
                              redirectedUrl(
                                      FRONTEND_REDIRECT_URL + "?code=" + code + "&state=" + state));
          }

          @Test
          @DisplayName("에러 발생 시 error와 함께 프론트엔드로 리다이렉트")
          void callback_WithError() throws Exception {
              // given
              String error = "access_denied";

              // when & then
              mockMvc.perform(get("/oauth2/callback/kakao").param("error", error))
                      .andExpect(status().is3xxRedirection())
                      .andExpect(redirectedUrl(FRONTEND_REDIRECT_URL + "?error=" + error));
          }

          @Test
          @DisplayName("code가 없을 때 no_code 에러로 리다이렉트")
          void callback_NoCode() throws Exception {
              // when & then
              mockMvc.perform(get("/oauth2/callback/kakao"))
                      .andExpect(status().is3xxRedirection())
                      .andExpect(redirectedUrl(FRONTEND_REDIRECT_URL + "?error=no_code"));
          }
      }

      @Nested
      @DisplayName("Authorization Code로 Access Token 교환")
      class ExchangeTokenTest {

          @Test
          @DisplayName("토큰 교환 성공")
          void exchangeToken_Success() throws Exception {
              // given
              String code = "test-auth-code";
              OAuth2TokenResponse tokenResponse =
                      OAuth2TokenResponse.builder()
                              .accessToken("test-access-token")
                              .tokenType("bearer")
                              .refreshToken("test-refresh-token")
                              .expiresIn(43199)
                              .scope("profile_nickname account_email")
                              .build();

              when(oauth2ManualService.exchangeCodeForToken(code)).thenReturn(tokenResponse);

              // when & then
              mockMvc.perform(post("/oauth2/token/exchange").param("code", code))
                      .andExpect(status().isOk())
                      .andExpect(jsonPath("$.success").value(true))
                      .andExpect(jsonPath("$.message").value("토큰 교환 성공"))
                      .andExpect(jsonPath("$.data.accessToken").value("test-access-token"))
                      .andExpect(jsonPath("$.data.tokenType").value("bearer"))
                      .andExpect(jsonPath("$.data.refreshToken").value("test-refresh-token"))
                      .andExpect(jsonPath("$.data.expiresIn").value(43199));
          }

          @Test
          @DisplayName("토큰 교환 실패")
          void exchangeToken_Failure() throws Exception {
              // given
              String code = "invalid-auth-code";
              when(oauth2ManualService.exchangeCodeForToken(code))
                      .thenThrow(new RuntimeException("Invalid authorization code"));

              // when & then
              mockMvc.perform(post("/oauth2/token/exchange").param("code", code))
                      .andExpect(status().isBadRequest())
                      .andExpect(jsonPath("$.success").value(false))
                      .andExpect(jsonPath("$.error.code").value("TOKEN_EXCHANGE_FAILED"))
                      .andExpect(
                              jsonPath("$.error.reason")
                                      .value("토큰 교환에 실패했습니다: Invalid authorization code"))
                      .andExpect(
                              jsonPath("$.message")
                                      .value("토큰 교환에 실패했습니다: Invalid authorization code"));
          }
      }

      @Nested
      @DisplayName("Access Token으로 사용자 정보 조회")
      class GetUserInfoTest {

          @Test
          @DisplayName("사용자 정보 조회 성공")
          void getUserInfo_Success() throws Exception {
              // given
              String accessToken = "test-access-token";
              OAuth2UserResponse userResponse =
                      OAuth2UserResponse.builder()
                              .id("123456789")
                              .email("test@example.com")
                              .nickname("테스트사용자")
                              .profileImageUrl("http://example.com/profile.jpg")
                              .gender("male")
                              .provider("kakao")
                              .build();

              when(oauth2ManualService.getUserInfo(accessToken)).thenReturn(userResponse);

              // when & then
              mockMvc.perform(post("/oauth2/user/info").param("accessToken", accessToken))
                      .andExpect(status().isOk())
                      .andExpect(jsonPath("$.success").value(true))
                      .andExpect(jsonPath("$.message").value("사용자 정보 조회 성공"))
                      .andExpect(jsonPath("$.data.id").value("123456789"))
                      .andExpect(jsonPath("$.data.email").value("test@example.com"))
                      .andExpect(jsonPath("$.data.nickname").value("테스트사용자"))
                      .andExpect(jsonPath("$.data.gender").value("male"));
          }

          @Test
          @DisplayName("사용자 정보 조회 실패")
          void getUserInfo_Failure() throws Exception {
              // given
              String accessToken = "invalid-access-token";
              when(oauth2ManualService.getUserInfo(accessToken))
                      .thenThrow(new RuntimeException("Invalid access token"));

              // when & then
              mockMvc.perform(post("/oauth2/user/info").param("accessToken", accessToken))
                      .andExpect(status().isBadRequest())
                      .andExpect(jsonPath("$.success").value(false))
                      .andExpect(jsonPath("$.error.code").value("USER_INFO_FAILED"));
          }
      }

      @Nested
      @DisplayName("OAuth2 완전 로그인 처리")
      class CompleteLoginTest {

          @Test
          @DisplayName("OAuth2 로그인 완료 성공")
          void completeLogin_Success() throws Exception {
              // given
              String code = "test-auth-code";
              Map<String, String> loginResult = new HashMap<>();
              loginResult.put("access_token", "jwt-access-token");
              loginResult.put("refresh_token", "jwt-refresh-token");
              loginResult.put("token_type", "Bearer");
              loginResult.put("user_id", "1");
              loginResult.put("username", "123456789");
              loginResult.put("nickname", "테스트사용자");
              loginResult.put("email", "test@example.com");
              loginResult.put("profile_image", "http://example.com/profile.jpg");
              loginResult.put("gender", "MALE");
              loginResult.put("role", "ROLE_USER");
              loginResult.put("expires_in", "86400");

              when(oauth2ManualService.processOAuth2Login(code, null)).thenReturn(loginResult);

              // when & then
              mockMvc.perform(post("/oauth2/login/complete").param("code", code))
                      .andExpect(status().isOk())
                      .andExpect(jsonPath("$.success").value(true))
                      .andExpect(jsonPath("$.message").value("OAuth2 로그인 완료"))
                      .andExpect(jsonPath("$.data.access_token").value("jwt-access-token"))
                      .andExpect(jsonPath("$.data.refresh_token").value("jwt-refresh-token"))
                      .andExpect(jsonPath("$.data.user_id").value("1"))
                      .andExpect(jsonPath("$.data.email").value("test@example.com"))
                      .andExpect(jsonPath("$.data.gender").value("MALE"));
          }

          @Test
          @DisplayName("OAuth2 로그인 실패")
          void completeLogin_Failure() throws Exception {
              // given
              String code = "invalid-auth-code";
              when(oauth2ManualService.processOAuth2Login(code))
                      .thenThrow(new RuntimeException("OAuth2 login failed"));

              // when & then
              mockMvc.perform(post("/oauth2/login/complete").param("code", code))
                      .andExpect(status().isBadRequest())
                      .andExpect(jsonPath("$.success").value(false))
                      .andExpect(jsonPath("$.error.code").value("LOGIN_FAILED"));
          }
      }

      @Nested
      @DisplayName("Access Token 새로고침")
      class RefreshTokenTest {

          @Test
          @DisplayName("토큰 새로고침 성공")
          void refreshToken_Success() throws Exception {
              // given
              String refreshToken = "test-refresh-token";
              OAuth2TokenResponse tokenResponse =
                      OAuth2TokenResponse.builder()
                              .accessToken("new-access-token")
                              .tokenType("bearer")
                              .refreshToken("new-refresh-token")
                              .expiresIn(43199)
                              .build();

              when(oauth2ManualService.refreshAccessToken(refreshToken)).thenReturn(tokenResponse);

              // when & then
              mockMvc.perform(post("/oauth2/token/refresh").param("refreshToken", refreshToken))
                      .andExpect(status().isOk())
                      .andExpect(jsonPath("$.success").value(true))
                      .andExpect(jsonPath("$.message").value("토큰 새로고침 성공"))
                      .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                      .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"));
          }

          @Test
          @DisplayName("토큰 새로고침 실패")
          void refreshToken_Failure() throws Exception {
              // given
              String refreshToken = "invalid-refresh-token";
              when(oauth2ManualService.refreshAccessToken(refreshToken))
                      .thenThrow(new RuntimeException("Invalid refresh token"));

              // when & then
              mockMvc.perform(post("/oauth2/token/refresh").param("refreshToken", refreshToken))
                      .andExpect(status().isBadRequest())
                      .andExpect(jsonPath("$.success").value(false))
                      .andExpect(jsonPath("$.error.code").value("TOKEN_REFRESH_FAILED"));
          }
      }
}
