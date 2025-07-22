package org.scoula.global.auth.service;

import org.scoula.global.auth.dto.AuthenticatedUserInfo;

/**
 * 인증 정보 서비스 인터페이스
 *
 * <p>JWT 토큰과 Spring Security를 통해 인증된 사용자의 정보를 제공하는 계약을 정의합니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
public interface AuthInfoServiceInterface {

      /**
       * JWT 토큰으로부터 사용자 정보 조회
       *
       * @param token JWT 액세스 토큰
       * @return 인증된 사용자 정보
       */
      AuthenticatedUserInfo getUserInfoFromToken(String token);

      /**
       * 현재 인증된 사용자 정보 조회 (SecurityContext에서)
       *
       * @return 인증된 사용자 정보
       */
      AuthenticatedUserInfo getCurrentUserInfo();

      /**
       * 토큰 유효성 검증
       *
       * @param token JWT 토큰
       * @return 토큰 유효성 정보
       */
      TokenValidationInfo validateTokenInfo(String token);

      /** 토큰 유효성 정보 */
      class TokenValidationInfo {
          private final boolean valid;
          private final boolean expired;
          private final boolean blacklisted;
          private final long remainingTime;

          public TokenValidationInfo(
                  boolean valid, boolean expired, boolean blacklisted, long remainingTime) {
              this.valid = valid;
              this.expired = expired;
              this.blacklisted = blacklisted;
              this.remainingTime = remainingTime;
          }

          public boolean isValid() {
              return valid;
          }

          public boolean isExpired() {
              return expired;
          }

          public boolean isBlacklisted() {
              return blacklisted;
          }

          public long getRemainingTime() {
              return remainingTime;
          }
      }
}
