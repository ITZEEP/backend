package org.scoula.global.oauth2.service;

import java.util.HashMap;
import java.util.Map;

import org.scoula.domain.user.service.UserServiceInterface;
import org.scoula.domain.user.vo.SocialAccount;
import org.scoula.domain.user.vo.User;
import org.scoula.global.auth.dto.KakaoUserInfo;
import org.scoula.global.auth.jwt.JwtUtil;
import org.scoula.global.common.util.HttpHeadersUtil;
import org.scoula.global.oauth2.dto.OAuth2TokenResponse;
import org.scoula.global.oauth2.dto.OAuth2UserResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * OAuth2 수동 처리 서비스
 *
 * <p>Spring Security 자동 처리 대신 수동으로 OAuth2 flow를 처리합니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class OAuth2ManualServiceImpl implements OAuth2ManualServiceInterface {

      private final RestTemplate restTemplate;
      private final JwtUtil jwtUtil;
      private final UserServiceInterface userService;

      @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
      private String kakaoClientId;

      @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
      private String kakaoClientSecret;

      @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
      private String kakaoRedirectUri;

      // Kakao OAuth2 엔드포인트
      private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
      private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

      /**
       * Authorization code로 access token 획득
       *
       * @param authorizationCode 인증 코드
       * @return OAuth2TokenResponse 토큰 응답
       */
      public OAuth2TokenResponse exchangeCodeForToken(String authorizationCode) {
          log.info("Authorization code를 access token으로 교환 시작");

          try {
              // 토큰 요청 헤더 설정
              HttpHeaders headers = HttpHeadersUtil.createFormUrlEncodedHeaders();

              // 토큰 요청 파라미터 설정
              MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
              params.add("grant_type", "authorization_code");
              params.add("client_id", kakaoClientId);
              params.add("client_secret", kakaoClientSecret);
              params.add("redirect_uri", kakaoRedirectUri);
              params.add("code", authorizationCode);

              HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

              // 카카오 토큰 서버에 요청
              ResponseEntity<Map> response =
                      restTemplate.exchange(KAKAO_TOKEN_URL, HttpMethod.POST, request, Map.class);

              Map<String, Object> responseBody = response.getBody();
              if (responseBody == null) {
                  throw new RuntimeException("토큰 응답이 비어있습니다");
              }

              log.info("Access token 획득 성공");

              return OAuth2TokenResponse.builder()
                      .accessToken((String) responseBody.get("access_token"))
                      .tokenType((String) responseBody.get("token_type"))
                      .refreshToken((String) responseBody.get("refresh_token"))
                      .expiresIn((Integer) responseBody.get("expires_in"))
                      .scope((String) responseBody.get("scope"))
                      .build();

          } catch (Exception e) {
              log.error("Access token 획득 실패", e);
              throw new RuntimeException("Access token 획득에 실패했습니다: " + e.getMessage());
          }
      }

      /**
       * Access token으로 사용자 정보 획득
       *
       * @param accessToken 액세스 토큰
       * @return OAuth2UserResponse 사용자 정보 응답
       */
      public OAuth2UserResponse getUserInfo(String accessToken) {
          log.info("Access token으로 사용자 정보 조회 시작");

          try {
              // 사용자 정보 요청 헤더 설정
              HttpHeaders headers = HttpHeadersUtil.createBearerAuthWithFormHeaders(accessToken);

              HttpEntity<String> request = new HttpEntity<>(headers);

              // 카카오 사용자 정보 API 호출
              ResponseEntity<Map> response =
                      restTemplate.exchange(KAKAO_USER_INFO_URL, HttpMethod.GET, request, Map.class);

              Map<String, Object> responseBody = response.getBody();
              if (responseBody == null) {
                  throw new RuntimeException("사용자 정보 응답이 비어있습니다");
              }

              log.info("사용자 정보 조회 성공");

              // KakaoUserInfo 객체로 변환
              KakaoUserInfo kakaoUserInfo = new KakaoUserInfo(responseBody);

              return OAuth2UserResponse.builder()
                      .id(kakaoUserInfo.getId())
                      .email(kakaoUserInfo.getEmail())
                      .nickname(kakaoUserInfo.getNickname())
                      .profileImageUrl(kakaoUserInfo.getProfileImageUrl())
                      .gender(kakaoUserInfo.getGender())
                      .provider("kakao")
                      .build();

          } catch (Exception e) {
              log.error("사용자 정보 조회 실패", e);
              throw new RuntimeException("사용자 정보 조회에 실패했습니다: " + e.getMessage());
          }
      }

      /**
       * OAuth2 완전 처리 (code -> token -> user info -> JWT)
       *
       * @param authorizationCode 인증 코드
       * @return JWT 토큰 정보
       */
      public Map<String, String> processOAuth2Login(String authorizationCode) {
          log.info("OAuth2 수동 로그인 처리 시작");

          try {
              // 1. Authorization code로 access token 획득
              OAuth2TokenResponse tokenResponse = exchangeCodeForToken(authorizationCode);

              // 2. Access token으로 사용자 정보 획득
              OAuth2UserResponse userResponse = getUserInfo(tokenResponse.getAccessToken());

              // 3. 데이터베이스에 사용자 등록 또는 업데이트
              User user =
                      userService.registerOrUpdateOAuth2User(
                              userResponse.getId(),
                              SocialAccount.SocialType.KAKAO,
                              userResponse.getEmail(),
                              userResponse.getNickname(),
                              userResponse.getProfileImageUrl(),
                              userResponse.getGender());

              log.info("사용자 처리 완료 - User ID: {}, Email: {}", user.getUserId(), user.getEmail());

              // 4. 자체 JWT 토큰 생성 (사용자 이메일 사용)
              String jwtAccessToken = jwtUtil.generateToken(user.getEmail());
              String jwtRefreshToken = jwtUtil.generateRefreshToken(user.getEmail());

              log.info("OAuth2 수동 로그인 처리 완료 - 사용자: {}", user.getUserId());

              Map<String, String> result = new HashMap<>();
              result.put("access_token", jwtAccessToken);
              result.put("refresh_token", jwtRefreshToken);
              result.put("token_type", "Bearer");
              result.put("user_id", String.valueOf(user.getUserId()));
              result.put("username", userResponse.getId());
              result.put("nickname", user.getNickname() != null ? user.getNickname() : "");
              result.put("email", user.getEmail() != null ? user.getEmail() : "");
              result.put(
                      "profile_image",
                      user.getProfileImgUrl() != null ? user.getProfileImgUrl() : "");
              result.put("gender", user.getGender() != null ? user.getGender().name() : "");
              result.put("role", user.getRole().name());
              result.put("expires_in", "86400"); // 24시간

              return result;

          } catch (Exception e) {
              log.error("OAuth2 수동 로그인 처리 실패", e);
              throw new RuntimeException("OAuth2 로그인 처리에 실패했습니다: " + e.getMessage());
          }
      }

      /**
       * Access token 새로고침
       *
       * @param refreshToken 리프레시 토큰
       * @return 새로운 토큰 응답
       */
      public OAuth2TokenResponse refreshAccessToken(String refreshToken) {
          log.info("Access token 새로고침 시작");

          try {
              HttpHeaders headers = HttpHeadersUtil.createFormUrlEncodedHeaders();

              MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
              params.add("grant_type", "refresh_token");
              params.add("client_id", kakaoClientId);
              params.add("client_secret", kakaoClientSecret);
              params.add("refresh_token", refreshToken);

              HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

              ResponseEntity<Map> response =
                      restTemplate.exchange(KAKAO_TOKEN_URL, HttpMethod.POST, request, Map.class);

              Map<String, Object> responseBody = response.getBody();
              if (responseBody == null) {
                  throw new RuntimeException("토큰 새로고침 응답이 비어있습니다");
              }

              log.info("Access token 새로고침 성공");

              return OAuth2TokenResponse.builder()
                      .accessToken((String) responseBody.get("access_token"))
                      .tokenType((String) responseBody.get("token_type"))
                      .refreshToken((String) responseBody.get("refresh_token"))
                      .expiresIn((Integer) responseBody.get("expires_in"))
                      .scope((String) responseBody.get("scope"))
                      .build();

          } catch (Exception e) {
              log.error("Access token 새로고침 실패", e);
              throw new RuntimeException("Access token 새로고침에 실패했습니다: " + e.getMessage());
          }
      }
}
