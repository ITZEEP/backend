package org.scoula.global.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("JWT 유틸리티 단위 테스트")
class JwtUtilTest {

      private JwtUtil jwtUtil;

      @BeforeEach
      void setUp() {
          jwtUtil = new JwtUtil();

          // 테스트용 설정값 주입
          ReflectionTestUtils.setField(
                  jwtUtil, "secret", "bXlTZWNyZXRLZXkxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=");
          ReflectionTestUtils.setField(jwtUtil, "expiration", 86400L); // 24시간 (초 단위)
          ReflectionTestUtils.setField(jwtUtil, "refreshExpiration", 604800L); // 7일 (초 단위)
          ReflectionTestUtils.setField(jwtUtil, "issuer", "itzeep");

          // init 메서드 호출하여 초기화 검증 통과
          jwtUtil.init();
      }

      @Test
      @DisplayName("JWT 토큰 생성 - 정상적인 토큰 반환")
      void generateToken_ShouldReturnValidToken() {
          // given
          String username = "testuser";

          // when
          String token = jwtUtil.generateToken(username);

          // then
          assertThat(token).isNotNull();
          assertThat(token).isNotEmpty();
          assertThat(token.split("\\.")).hasSize(3); // JWT는 3개 부분으로 구성
      }

      @Test
      @DisplayName("사용자명 추출 - 토큰에서 올바른 사용자명 반환")
      void getUsername_ShouldReturnCorrectUsername() {
          // given
          String username = "testuser";
          String token = jwtUtil.generateToken(username);

          // when
          String extractedUsername = jwtUtil.getUsername(token);

          // then
          assertThat(extractedUsername).isEqualTo(username);
      }

      @Test
      @DisplayName("토큰 검증 - 유효한 토큰에 대해 true 반환")
      void validateToken_ShouldReturnTrueForValidToken() {
          // given
          String username = "testuser";
          String token = jwtUtil.generateToken(username);

          // when
          boolean isValid = jwtUtil.validateToken(token);

          // then
          assertThat(isValid).isTrue();
      }

      @Test
      @DisplayName("잘못된 토큰 검증 - 유효하지 않은 토큰에 대해 false 반환")
      void validateToken_ShouldReturnFalseForInvalidToken() {
          // given
          String invalidToken = "invalid.token.here";

          // when
          boolean isValid = jwtUtil.validateToken(invalidToken);

          // then
          assertThat(isValid).isFalse();
      }
}
