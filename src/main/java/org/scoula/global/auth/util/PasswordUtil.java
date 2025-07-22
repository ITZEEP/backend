package org.scoula.global.auth.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 비밀번호 유틸리티 클래스
 *
 * <p>OAuth2 사용자를 위한 비밀번호 생성 및 암호화 기능을 제공합니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@Component
public class PasswordUtil {

      private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

      /**
       * OAuth2 사용자를 위한 비밀번호 생성
       *
       * @param socialId 소셜 ID
       * @return BCrypt로 암호화된 비밀번호
       */
      public static String generateOAuth2Password(String socialId) {
          // social_id를 기반으로 비밀번호 생성
          String rawPassword = "oauth2_" + socialId + "_secret";
          return encoder.encode(rawPassword);
      }

      /**
       * 비밀번호 암호화
       *
       * @param rawPassword 원본 비밀번호
       * @return BCrypt로 암호화된 비밀번호
       */
      public static String encode(String rawPassword) {
          return encoder.encode(rawPassword);
      }
}
