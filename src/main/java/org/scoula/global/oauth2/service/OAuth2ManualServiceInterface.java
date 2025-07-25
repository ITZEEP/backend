package org.scoula.global.oauth2.service;

import java.util.Map;

import org.scoula.global.oauth2.dto.OAuth2TokenResponse;
import org.scoula.global.oauth2.dto.OAuth2UserResponse;

/**
 * OAuth2 수동 처리 서비스 인터페이스
 *
 * <p>Spring Security 자동 처리 대신 수동으로 OAuth2 flow를 처리하는 계약을 정의합니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
public interface OAuth2ManualServiceInterface {

      /**
       * Authorization code로 access token 획득
       *
       * @param authorizationCode 인증 코드
       * @return OAuth2TokenResponse 토큰 응답
       */
      OAuth2TokenResponse exchangeCodeForToken(String authorizationCode);

      /**
       * Authorization code로 access token 획득 (redirect URI 지정)
       *
       * @param authorizationCode 인증 코드
       * @param redirectUri 리다이렉트 URI
       * @return OAuth2TokenResponse 토큰 응답
       */
      OAuth2TokenResponse exchangeCodeForToken(String authorizationCode, String redirectUri);

      /**
       * Access token으로 사용자 정보 획득
       *
       * @param accessToken 액세스 토큰
       * @return OAuth2UserResponse 사용자 정보 응답
       */
      OAuth2UserResponse getUserInfo(String accessToken);

      /**
       * OAuth2 완전 처리 (code -> token -> user info -> JWT)
       *
       * @param authorizationCode 인증 코드
       * @return JWT 토큰 정보
       */
      Map<String, String> processOAuth2Login(String authorizationCode);

      /**
       * OAuth2 완전 처리 (code -> token -> user info -> JWT) (redirect URI 지정)
       *
       * @param authorizationCode 인증 코드
       * @param redirectUri 리다이렉트 URI
       * @return JWT 토큰 정보
       */
      Map<String, String> processOAuth2Login(String authorizationCode, String redirectUri);

      /**
       * Access token 새로고침
       *
       * @param refreshToken 리프레시 토큰
       * @return 새로운 토큰 응답
       */
      OAuth2TokenResponse refreshAccessToken(String refreshToken);
}
