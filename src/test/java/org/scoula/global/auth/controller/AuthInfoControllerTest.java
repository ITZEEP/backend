package org.scoula.global.auth.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scoula.global.auth.dto.AuthenticatedUserInfo;
import org.scoula.global.auth.service.AuthInfoServiceInterface;
import org.scoula.global.common.exception.BusinessException;
import org.scoula.global.common.exception.CommonErrorCode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * AuthInfoController 단위 테스트
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthInfoController 단위 테스트")
class AuthInfoControllerTest {

      @Mock private AuthInfoServiceInterface authInfoService;

      @InjectMocks private AuthInfoControllerImpl authInfoController;

      private MockMvc mockMvc;

      @BeforeEach
      void setUp() {
          mockMvc = MockMvcBuilders.standaloneSetup(authInfoController).build();
      }

      @Nested
      @DisplayName("현재 인증된 사용자 정보 조회")
      class GetCurrentUserTest {

          @Test
          @DisplayName("인증된 사용자 정보 조회 성공")
          void getCurrentUser_Success() throws Exception {
              // given
              AuthenticatedUserInfo userInfo =
                      AuthenticatedUserInfo.builder()
                              .username("1")
                              .userId("1")
                              .email("test@example.com")
                              .nickname("테스트사용자")
                              .profileImageUrl("http://example.com/profile.jpg")
                              .gender("MALE")
                              .authorities(Arrays.asList("ROLE_USER"))
                              .issuedAt("2024-01-01 12:00:00")
                              .expiresAt("2024-01-02 12:00:00")
                              .remainingTime(86400L)
                              .provider("kakao")
                              .enabled(true)
                              .locked(false)
                              .build();

              when(authInfoService.getCurrentUserInfo()).thenReturn(userInfo);

              // when & then
              mockMvc.perform(get("/api/auth/info/current"))
                      .andExpect(status().isOk())
                      .andExpect(jsonPath("$.success").value(true))
                      .andExpect(jsonPath("$.message").value("사용자 정보 조회 성공"))
                      .andExpect(jsonPath("$.data.username").value("1"))
                      .andExpect(jsonPath("$.data.email").value("test@example.com"))
                      .andExpect(jsonPath("$.data.nickname").value("테스트사용자"))
                      .andExpect(jsonPath("$.data.gender").value("MALE"))
                      .andExpect(jsonPath("$.data.provider").value("kakao"));
          }

          @Test
          @DisplayName("인증되지 않은 사용자 예외 발생")
          void getCurrentUser_NotAuthenticated() throws Exception {
              // given
              when(authInfoService.getCurrentUserInfo())
                      .thenThrow(
                              new BusinessException(
                                      CommonErrorCode.AUTHENTICATION_FAILED, "인증되지 않은 사용자입니다"));

              // when & then
              mockMvc.perform(get("/api/auth/info/current"))
                      .andExpect(status().isUnauthorized())
                      .andExpect(jsonPath("$.success").value(false))
                      .andExpect(jsonPath("$.error.code").value("AUTH_ERROR"))
                      .andExpect(jsonPath("$.error.reason").value("인증되지 않은 사용자입니다"))
                      .andExpect(jsonPath("$.message").value("인증되지 않은 사용자입니다"));
          }
      }

      /* These tests are commented out as the endpoints don't exist in the current controller
      @Nested
      @DisplayName("JWT 토큰으로 사용자 정보 조회")
      class VerifyTokenTest {

          @Test
          @DisplayName("유효한 토큰으로 사용자 정보 조회 성공")
          void verifyToken_Success() throws Exception {
              // given
              String token = "valid.jwt.token";
              AuthenticatedUserInfo userInfo =
                      AuthenticatedUserInfo.builder()
                              .username("1")
                              .userId("1")
                              .email("test@example.com")
                              .nickname("테스트사용자")
                              .authorities(Arrays.asList("ROLE_USER"))
                              .enabled(true)
                              .locked(false)
                              .build();

              when(authInfoService.getUserInfoFromToken(token)).thenReturn(userInfo);

              // when & then
              mockMvc.perform(post("/api/auth/info/verify").param("token", token))
                      .andExpect(status().isOk())
                      .andExpect(jsonPath("$.success").value(true))
                      .andExpect(jsonPath("$.message").value("토큰 검증 및 사용자 정보 조회 성공"))
                      .andExpect(jsonPath("$.data.username").value("1"))
                      .andExpect(jsonPath("$.data.email").value("test@example.com"));
          }

          @Test
          @DisplayName("유효하지 않은 토큰으로 예외 발생")
          void verifyToken_InvalidToken() throws Exception {
              // given
              String token = "invalid.jwt.token";
              when(authInfoService.getUserInfoFromToken(token))
                      .thenThrow(
                              new BusinessException(CommonErrorCode.INVALID_TOKEN, "유효하지 않은 토큰입니다"));

              // when & then
              mockMvc.perform(post("/api/auth/info/verify").param("token", token))
                      .andExpect(status().isBadRequest())
                      .andExpect(jsonPath("$.success").value(false))
                      .andExpect(jsonPath("$.error.code").value("TOKEN_VERIFICATION_FAILED"));
          }
      }
      @Nested
      @DisplayName("JWT 토큰 유효성 검증")
      class ValidateTokenTest {

          @Test
          @DisplayName("토큰 유효성 검증 성공")
          void validateToken_Success() throws Exception {
              // given
              String token = "valid.jwt.token";
              AuthInfoService.TokenValidationInfo validationInfo =
                      AuthInfoService.TokenValidationInfo.builder()
                              .valid(true)
                              .expired(false)
                              .blacklisted(false)
                              .remainingTime(3600L)
                              .build();

              when(authInfoService.validateTokenInfo(token)).thenReturn(validationInfo);

              // when & then
              mockMvc.perform(post("/api/auth/info/validate").param("token", token))
                      .andExpect(status().isOk())
                      .andExpect(jsonPath("$.success").value(true))
                      .andExpect(jsonPath("$.message").value("토큰 유효성 검증 완료"))
                      .andExpect(jsonPath("$.data.valid").value(true))
                      .andExpect(jsonPath("$.data.expired").value(false))
                      .andExpect(jsonPath("$.data.blacklisted").value(false))
                      .andExpect(jsonPath("$.data.remainingTime").value(3600));
          }

          @Test
          @DisplayName("만료된 토큰 검증")
          void validateToken_Expired() throws Exception {
              // given
              String token = "expired.jwt.token";
              AuthInfoService.TokenValidationInfo validationInfo =
                      AuthInfoService.TokenValidationInfo.builder()
                              .valid(false)
                              .expired(true)
                              .blacklisted(false)
                              .remainingTime(0L)
                              .build();

              when(authInfoService.validateTokenInfo(token)).thenReturn(validationInfo);

              // when & then
              mockMvc.perform(post("/api/auth/info/validate").param("token", token))
                      .andExpect(status().isOk())
                      .andExpect(jsonPath("$.data.valid").value(false))
                      .andExpect(jsonPath("$.data.expired").value(true));
          }
      }

      @Nested
      @DisplayName("Authorization 헤더로 사용자 정보 조회")
      class GetUserInfoFromHeaderTest {

          @Test
          @DisplayName("Bearer 토큰으로 사용자 정보 조회 성공")
          void getUserInfoFromHeader_Success() throws Exception {
              // given
              String token = "valid.jwt.token";
              String authHeader = "Bearer " + token;

              AuthenticatedUserInfo userInfo =
                      AuthenticatedUserInfo.builder()
                              .username("1")
                              .userId("1")
                              .email("test@example.com")
                              .nickname("테스트사용자")
                              .build();

              when(authInfoService.getUserInfoFromToken(token)).thenReturn(userInfo);

              // when & then
              mockMvc.perform(get("/api/auth/info/header").header("Authorization", authHeader))
                      .andExpect(status().isOk())
                      .andExpect(jsonPath("$.success").value(true))
                      .andExpect(jsonPath("$.message").value("사용자 정보 조회 성공"))
                      .andExpect(jsonPath("$.data.username").value("1"));
          }

          @Test
          @DisplayName("Bearer 접두사 없는 토큰으로도 조회 성공")
          void getUserInfoFromHeader_WithoutBearerPrefix() throws Exception {
              // given
              String token = "valid.jwt.token";

              AuthenticatedUserInfo userInfo =
                      AuthenticatedUserInfo.builder().username("1").userId("1").build();

              when(authInfoService.getUserInfoFromToken(token)).thenReturn(userInfo);

              // when & then
              mockMvc.perform(get("/api/auth/info/header").header("Authorization", token))
                      .andExpect(status().isOk())
                      .andExpect(jsonPath("$.success").value(true));
          }
      }

      @Nested
      @DisplayName("인증 상태 확인")
      class GetAuthStatusTest {

          @Test
          @DisplayName("인증된 상태 확인")
          void getAuthStatus_Authenticated() throws Exception {
              // when & then - With standalone setup, this will return no authentication
              mockMvc.perform(get("/api/auth/info/auth-status"))
                      .andExpect(status().isOk())
                      .andExpect(jsonPath("$.success").value(true))
                      .andExpect(jsonPath("$.message").value("인증 상태 확인 완료"))
                      .andExpect(jsonPath("$.data.authenticated").value(false))
                      .andExpect(jsonPath("$.data.message").value("인증 정보가 없습니다"));
          }

          @Test
          @DisplayName("인증되지 않은 상태 확인")
          void getAuthStatus_NotAuthenticated() throws Exception {
              // given
              // SecurityContext가 비어있는 상태

              // when & then
              mockMvc.perform(get("/api/auth/info/auth-status"))
                      .andExpect(status().isOk())
                      .andExpect(jsonPath("$.success").value(true))
                      .andExpect(jsonPath("$.data.authenticated").value(false))
                      .andExpect(jsonPath("$.data.message").value("인증 정보가 없습니다"));
          }
      }
      */
}
