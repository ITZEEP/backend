package org.scoula.global.oauth2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scoula.domain.user.service.UserServiceInterface;
import org.scoula.domain.user.vo.SocialAccount;
import org.scoula.domain.user.vo.User;
import org.scoula.global.auth.jwt.JwtUtil;
import org.scoula.global.oauth2.dto.OAuth2TokenResponse;
import org.scoula.global.oauth2.dto.OAuth2UserResponse;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * OAuth2ManualService 단위 테스트
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2ManualService 단위 테스트")
class OAuth2ManualServiceTest {

      @Mock private RestTemplate restTemplate;

      @Mock private JwtUtil jwtUtil;

      @Mock private UserServiceInterface userService;

      private OAuth2ManualServiceInterface oauth2ManualService;

      private static final String KAKAO_CLIENT_ID = "test-client-id";
      private static final String KAKAO_CLIENT_SECRET = "test-client-secret";
      private static final String KAKAO_REDIRECT_URI = "http://localhost:3000/oauth/callback/kakao";

      @BeforeEach
      void setUp() {
          oauth2ManualService = new OAuth2ManualServiceImpl(restTemplate, jwtUtil, userService);

          // @Value 필드 주입
          ReflectionTestUtils.setField(oauth2ManualService, "kakaoClientId", KAKAO_CLIENT_ID);
          ReflectionTestUtils.setField(oauth2ManualService, "kakaoClientSecret", KAKAO_CLIENT_SECRET);
          ReflectionTestUtils.setField(oauth2ManualService, "kakaoRedirectUri", KAKAO_REDIRECT_URI);
      }

      @Nested
      @DisplayName("Authorization Code로 Access Token 교환")
      class ExchangeCodeForTokenTest {

          @Test
          @DisplayName("Authorization Code로 Access Token 획득 성공")
          void exchangeCodeForToken_Success() {
              // given
              String authorizationCode = "test-auth-code";
              Map<String, Object> tokenResponse = new HashMap<>();
              tokenResponse.put("access_token", "test-access-token");
              tokenResponse.put("token_type", "bearer");
              tokenResponse.put("refresh_token", "test-refresh-token");
              tokenResponse.put("expires_in", 43199);
              tokenResponse.put("scope", "profile_nickname account_email");

              ResponseEntity<Map> responseEntity = new ResponseEntity<>(tokenResponse, HttpStatus.OK);
              when(restTemplate.exchange(
                              anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                      .thenReturn(responseEntity);

              // when
              OAuth2TokenResponse result =
                      oauth2ManualService.exchangeCodeForToken(authorizationCode);

              // then
              assertThat(result).isNotNull();
              assertThat(result.getAccessToken()).isEqualTo("test-access-token");
              assertThat(result.getTokenType()).isEqualTo("bearer");
              assertThat(result.getRefreshToken()).isEqualTo("test-refresh-token");
              assertThat(result.getExpiresIn()).isEqualTo(43199);
              assertThat(result.getScope()).isEqualTo("profile_nickname account_email");

              verify(restTemplate)
                      .exchange(
                              eq("https://kauth.kakao.com/oauth/token"),
                              eq(HttpMethod.POST),
                              argThat(
                                      entity -> {
                                          MultiValueMap<String, String> body =
                                                  (MultiValueMap<String, String>) entity.getBody();
                                          return body.getFirst("grant_type")
                                                          .equals("authorization_code")
                                                  && body.getFirst("client_id")
                                                          .equals(KAKAO_CLIENT_ID)
                                                  && body.getFirst("client_secret")
                                                          .equals(KAKAO_CLIENT_SECRET)
                                                  && body.getFirst("redirect_uri")
                                                          .equals(KAKAO_REDIRECT_URI)
                                                  && body.getFirst("code").equals(authorizationCode);
                                      }),
                              eq(Map.class));
          }

          @Test
          @DisplayName("토큰 응답이 비어있을 때 예외 발생")
          void exchangeCodeForToken_EmptyResponse_ThrowsException() {
              // given
              String authorizationCode = "test-auth-code";
              ResponseEntity<Map> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);
              when(restTemplate.exchange(
                              anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                      .thenReturn(responseEntity);

              // when & then
              assertThatThrownBy(() -> oauth2ManualService.exchangeCodeForToken(authorizationCode))
                      .isInstanceOf(RuntimeException.class)
                      .hasMessageContaining("토큰 응답이 비어있습니다");
          }

          @Test
          @DisplayName("RestTemplate 예외 발생 시 처리")
          void exchangeCodeForToken_RestTemplateException() {
              // given
              String authorizationCode = "test-auth-code";
              when(restTemplate.exchange(
                              anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                      .thenThrow(new RuntimeException("Network error"));

              // when & then
              assertThatThrownBy(() -> oauth2ManualService.exchangeCodeForToken(authorizationCode))
                      .isInstanceOf(RuntimeException.class)
                      .hasMessageContaining("Access token 획득에 실패했습니다");
          }
      }

      @Nested
      @DisplayName("Access Token으로 사용자 정보 획득")
      class GetUserInfoTest {

          @Test
          @DisplayName("Access Token으로 사용자 정보 조회 성공")
          void getUserInfo_Success() {
              // given
              String accessToken = "test-access-token";
              Map<String, Object> userInfoResponse = createKakaoUserResponse();

              ResponseEntity<Map> responseEntity =
                      new ResponseEntity<>(userInfoResponse, HttpStatus.OK);
              when(restTemplate.exchange(
                              anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                      .thenReturn(responseEntity);

              // when
              OAuth2UserResponse result = oauth2ManualService.getUserInfo(accessToken);

              // then
              assertThat(result).isNotNull();
              assertThat(result.getId()).isEqualTo("123456789");
              assertThat(result.getEmail()).isEqualTo("test@example.com");
              assertThat(result.getNickname()).isEqualTo("테스트사용자");
              assertThat(result.getProfileImageUrl()).isEqualTo("http://example.com/profile.jpg");
              assertThat(result.getGender()).isEqualTo("male");
              assertThat(result.getProvider()).isEqualTo("kakao");

              verify(restTemplate)
                      .exchange(
                              eq("https://kapi.kakao.com/v2/user/me"),
                              eq(HttpMethod.GET),
                              argThat(
                                      entity -> {
                                          HttpHeaders headers = entity.getHeaders();
                                          return headers.getFirst(HttpHeaders.AUTHORIZATION)
                                                  .equals("Bearer " + accessToken);
                                      }),
                              eq(Map.class));
          }

          @Test
          @DisplayName("사용자 정보 응답이 비어있을 때 예외 발생")
          void getUserInfo_EmptyResponse_ThrowsException() {
              // given
              String accessToken = "test-access-token";
              ResponseEntity<Map> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);
              when(restTemplate.exchange(
                              anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                      .thenReturn(responseEntity);

              // when & then
              assertThatThrownBy(() -> oauth2ManualService.getUserInfo(accessToken))
                      .isInstanceOf(RuntimeException.class)
                      .hasMessageContaining("사용자 정보 응답이 비어있습니다");
          }
      }

      @Nested
      @DisplayName("OAuth2 전체 처리 (code -> token -> user info -> JWT)")
      class ProcessOAuth2LoginTest {

          @Test
          @DisplayName("OAuth2 로그인 전체 처리 성공")
          void processOAuth2Login_Success() {
              // given
              String authorizationCode = "test-auth-code";
              String accessToken = "test-access-token";
              String jwtAccessToken = "jwt-access-token";
              String jwtRefreshToken = "jwt-refresh-token";

              // 토큰 응답 설정
              Map<String, Object> tokenResponse = new HashMap<>();
              tokenResponse.put("access_token", accessToken);
              tokenResponse.put("token_type", "bearer");
              tokenResponse.put("refresh_token", "test-refresh-token");
              tokenResponse.put("expires_in", 43199);

              ResponseEntity<Map> tokenResponseEntity =
                      new ResponseEntity<>(tokenResponse, HttpStatus.OK);

              // 사용자 정보 응답 설정
              Map<String, Object> userInfoResponse = createKakaoUserResponse();
              ResponseEntity<Map> userInfoResponseEntity =
                      new ResponseEntity<>(userInfoResponse, HttpStatus.OK);

              // 사용자 엔티티 설정
              User user =
                      User.builder()
                              .userId(1L)
                              .email("test@example.com")
                              .nickname("테스트사용자")
                              .profileImgUrl("http://example.com/profile.jpg")
                              .gender(User.Gender.MALE)
                              .role(User.Role.ROLE_USER)
                              .build();

              when(restTemplate.exchange(
                              eq("https://kauth.kakao.com/oauth/token"),
                              eq(HttpMethod.POST),
                              any(HttpEntity.class),
                              eq(Map.class)))
                      .thenReturn(tokenResponseEntity);

              when(restTemplate.exchange(
                              eq("https://kapi.kakao.com/v2/user/me"),
                              eq(HttpMethod.GET),
                              any(HttpEntity.class),
                              eq(Map.class)))
                      .thenReturn(userInfoResponseEntity);

              when(userService.registerOrUpdateOAuth2User(
                              eq("123456789"),
                              eq(SocialAccount.SocialType.KAKAO),
                              eq("test@example.com"),
                              eq("테스트사용자"),
                              eq("http://example.com/profile.jpg"),
                              eq("male")))
                      .thenReturn(user);

              when(jwtUtil.generateToken("test@example.com")).thenReturn(jwtAccessToken);
              when(jwtUtil.generateRefreshToken("test@example.com")).thenReturn(jwtRefreshToken);
              when(jwtUtil.getAccessTokenExpiration()).thenReturn(86400L);

              // when
              Map<String, String> result = oauth2ManualService.processOAuth2Login(authorizationCode);

              // then
              assertThat(result).isNotNull();
              assertThat(result.get("access_token")).isEqualTo(jwtAccessToken);
              assertThat(result.get("refresh_token")).isEqualTo(jwtRefreshToken);
              assertThat(result.get("token_type")).isEqualTo("Bearer");
              assertThat(result.get("user_id")).isEqualTo("1");
              assertThat(result.get("username")).isEqualTo("123456789");
              assertThat(result.get("nickname")).isEqualTo("테스트사용자");
              assertThat(result.get("email")).isEqualTo("test@example.com");
              assertThat(result.get("profile_image")).isEqualTo("http://example.com/profile.jpg");
              assertThat(result.get("gender")).isEqualTo("MALE");
              assertThat(result.get("role")).isEqualTo("ROLE_USER");
              assertThat(result.get("expires_in")).isEqualTo("86400");

              verify(userService)
                      .registerOrUpdateOAuth2User(
                              "123456789",
                              SocialAccount.SocialType.KAKAO,
                              "test@example.com",
                              "테스트사용자",
                              "http://example.com/profile.jpg",
                              "male");
          }

          @Test
          @DisplayName("토큰 획득 실패 시 예외 발생")
          void processOAuth2Login_TokenExchangeFailed() {
              // given
              String authorizationCode = "test-auth-code";
              when(restTemplate.exchange(
                              eq("https://kauth.kakao.com/oauth/token"),
                              eq(HttpMethod.POST),
                              any(HttpEntity.class),
                              eq(Map.class)))
                      .thenThrow(new RuntimeException("Token exchange failed"));

              // when & then
              assertThatThrownBy(() -> oauth2ManualService.processOAuth2Login(authorizationCode))
                      .isInstanceOf(RuntimeException.class)
                      .hasMessageContaining("OAuth2 로그인 처리에 실패했습니다");
          }
      }

      @Nested
      @DisplayName("Access Token 새로고침")
      class RefreshAccessTokenTest {

          @Test
          @DisplayName("Refresh Token으로 Access Token 새로고침 성공")
          void refreshAccessToken_Success() {
              // given
              String refreshToken = "test-refresh-token";
              Map<String, Object> tokenResponse = new HashMap<>();
              tokenResponse.put("access_token", "new-access-token");
              tokenResponse.put("token_type", "bearer");
              tokenResponse.put("refresh_token", "new-refresh-token");
              tokenResponse.put("expires_in", 43199);

              ResponseEntity<Map> responseEntity = new ResponseEntity<>(tokenResponse, HttpStatus.OK);
              when(restTemplate.exchange(
                              anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                      .thenReturn(responseEntity);

              // when
              OAuth2TokenResponse result = oauth2ManualService.refreshAccessToken(refreshToken);

              // then
              assertThat(result).isNotNull();
              assertThat(result.getAccessToken()).isEqualTo("new-access-token");
              assertThat(result.getRefreshToken()).isEqualTo("new-refresh-token");

              verify(restTemplate)
                      .exchange(
                              eq("https://kauth.kakao.com/oauth/token"),
                              eq(HttpMethod.POST),
                              argThat(
                                      entity -> {
                                          MultiValueMap<String, String> body =
                                                  (MultiValueMap<String, String>) entity.getBody();
                                          return body.getFirst("grant_type").equals("refresh_token")
                                                  && body.getFirst("refresh_token")
                                                          .equals(refreshToken);
                                      }),
                              eq(Map.class));
          }
      }

      // Helper method
      private Map<String, Object> createKakaoUserResponse() {
          Map<String, Object> response = new HashMap<>();
          response.put("id", 123456789);

          Map<String, Object> kakaoAccount = new HashMap<>();
          kakaoAccount.put("email", "test@example.com");
          kakaoAccount.put("gender", "male");

          Map<String, Object> profile = new HashMap<>();
          profile.put("nickname", "테스트사용자");
          profile.put("profile_image_url", "http://example.com/profile.jpg");

          kakaoAccount.put("profile", profile);
          response.put("kakao_account", kakaoAccount);

          return response;
      }
}
