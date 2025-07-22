package org.scoula.global.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scoula.domain.user.mapper.UserMapper;
import org.scoula.domain.user.vo.User;
import org.scoula.global.auth.dto.AuthenticatedUserInfo;
import org.scoula.global.auth.jwt.JwtUtil;
import org.scoula.global.auth.util.UserInfoExtractor;
import org.scoula.global.common.exception.BusinessException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * AuthInfoService 단위 테스트
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthInfoService 단위 테스트")
class AuthInfoServiceTest {

      @Mock private JwtUtil jwtUtil;

      @Mock private CustomUserDetailsService userDetailsService;

      @Mock private UserMapper userMapper;

      @Mock private UserInfoExtractor userInfoExtractor;

      @Mock private SecurityContext securityContext;

      @Mock private UserDetails userDetails;

      private AuthInfoServiceInterface authInfoService;

      @BeforeEach
      void setUp() {
          authInfoService =
                  new AuthInfoServiceImpl(jwtUtil, userDetailsService, userMapper, userInfoExtractor);
          SecurityContextHolder.setContext(securityContext);
      }

      @Nested
      @DisplayName("JWT 토큰으로부터 사용자 정보 조회")
      class GetUserInfoFromTokenTest {

          @Test
          @DisplayName("유효한 토큰으로 사용자 정보 조회 성공")
          void getUserInfoFromToken_Success() {
              // given
              String token = "valid.jwt.token";
              String username = "1";
              Long userId = 1L;

              User user =
                      User.builder()
                              .userId(userId)
                              .email("test@example.com")
                              .nickname("테스트사용자")
                              .profileImgUrl("http://example.com/profile.jpg")
                              .gender(User.Gender.MALE)
                              .role(User.Role.ROLE_USER)
                              .build();

              JwtUtil.TokenInfo tokenInfo = new JwtUtil.TokenInfo();
              tokenInfo.setIssuedAt(Date.from(Instant.now().minusSeconds(3600)));
              tokenInfo.setExpiresAt(Date.from(Instant.now().plusSeconds(3600)));

              when(jwtUtil.validateToken(token)).thenReturn(true);
              when(jwtUtil.extractUsername(token)).thenReturn(username);
              when(jwtUtil.getTokenInfo(token)).thenReturn(tokenInfo);
              when(jwtUtil.getRemainingTime(token)).thenReturn(3600000L);
              when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
              when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());
              when(userDetails.isEnabled()).thenReturn(true);
              when(userDetails.isAccountNonLocked()).thenReturn(true);

              UserInfoExtractor.UserInfo userInfo =
                      UserInfoExtractor.UserInfo.builder()
                              .user(user)
                              .genderStr(user.getGender().name())
                              .build();
              when(userInfoExtractor.extractUserInfo(username)).thenReturn(userInfo);

              // when
              AuthenticatedUserInfo result = authInfoService.getUserInfoFromToken(token);

              // then
              assertThat(result).isNotNull();
              assertThat(result.getUsername()).isEqualTo(username);
              assertThat(result.getUserId()).isEqualTo("1");
              assertThat(result.getEmail()).isEqualTo("test@example.com");
              assertThat(result.getNickname()).isEqualTo("테스트사용자");
              assertThat(result.getProfileImageUrl()).isEqualTo("http://example.com/profile.jpg");
              assertThat(result.getGender()).isEqualTo("MALE");
              assertThat(result.getAuthorities()).isEmpty();
              assertThat(result.getEnabled()).isTrue();
              assertThat(result.getLocked()).isFalse();
              assertThat(result.getRemainingTime()).isEqualTo(3600L);
          }

          @Test
          @DisplayName("유효하지 않은 토큰으로 예외 발생")
          void getUserInfoFromToken_InvalidToken_ThrowsException() {
              // given
              String token = "invalid.jwt.token";
              when(jwtUtil.validateToken(token)).thenReturn(false);

              // when & then
              assertThatThrownBy(() -> authInfoService.getUserInfoFromToken(token))
                      .isInstanceOf(BusinessException.class)
                      .hasMessageContaining("유효하지 않은 토큰입니다");
          }

          @Test
          @DisplayName("사용자 ID가 숫자가 아닌 경우에도 처리")
          void getUserInfoFromToken_NonNumericUserId() {
              // given
              String token = "valid.jwt.token";
              String username = "oauth2_user";

              when(jwtUtil.validateToken(token)).thenReturn(true);
              when(jwtUtil.extractUsername(token)).thenReturn(username);

              // 예외가 발생하기 전에 호출되는 메서드들
              JwtUtil.TokenInfo tokenInfo = new JwtUtil.TokenInfo();
              tokenInfo.setIssuedAt(Date.from(Instant.now().minusSeconds(3600)));
              tokenInfo.setExpiresAt(Date.from(Instant.now().plusSeconds(3600)));
              when(jwtUtil.getTokenInfo(token)).thenReturn(tokenInfo);
              when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);

              UserInfoExtractor.UserInfo userInfo =
                      UserInfoExtractor.UserInfo.builder().user(null).genderStr(null).build();
              when(userInfoExtractor.extractUserInfo(username)).thenReturn(userInfo);

              // when & then - 사용자 정보가 없을 때 예외 발생 예상
              assertThatThrownBy(() -> authInfoService.getUserInfoFromToken(token))
                      .isInstanceOf(BusinessException.class)
                      .hasMessageContaining("사용자 정보를 찾을 수 없습니다");
          }

          @Test
          @DisplayName("DB에서 사용자를 찾을 수 없는 경우")
          void getUserInfoFromToken_UserNotFoundInDB() {
              // given
              String token = "valid.jwt.token";
              String username = "1";

              when(jwtUtil.validateToken(token)).thenReturn(true);
              when(jwtUtil.extractUsername(token)).thenReturn(username);

              // 예외가 발생하기 전에 호출되는 메서드들
              JwtUtil.TokenInfo tokenInfo = new JwtUtil.TokenInfo();
              tokenInfo.setIssuedAt(Date.from(Instant.now().minusSeconds(3600)));
              tokenInfo.setExpiresAt(Date.from(Instant.now().plusSeconds(3600)));
              when(jwtUtil.getTokenInfo(token)).thenReturn(tokenInfo);
              when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);

              UserInfoExtractor.UserInfo userInfo =
                      UserInfoExtractor.UserInfo.builder().user(null).genderStr(null).build();
              when(userInfoExtractor.extractUserInfo(username)).thenReturn(userInfo);

              // when & then - 사용자 정보가 없을 때 예외 발생 예상
              assertThatThrownBy(() -> authInfoService.getUserInfoFromToken(token))
                      .isInstanceOf(BusinessException.class)
                      .hasMessageContaining("사용자 정보를 찾을 수 없습니다");
          }
      }

      @Nested
      @DisplayName("현재 인증된 사용자 정보 조회")
      class GetCurrentUserInfoTest {

          @Test
          @DisplayName("인증된 사용자 정보 조회 성공")
          void getCurrentUserInfo_Success() {
              // given
              String username = "1";
              Long userId = 1L;

              User user =
                      User.builder()
                              .userId(userId)
                              .email("test@example.com")
                              .nickname("테스트사용자")
                              .profileImgUrl("http://example.com/profile.jpg")
                              .gender(User.Gender.FEMALE)
                              .role(User.Role.ROLE_USER)
                              .build();

              Authentication authentication =
                      new UsernamePasswordAuthenticationToken(
                              username,
                              "password",
                              Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")));

              when(securityContext.getAuthentication()).thenReturn(authentication);
              when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
              when(userDetails.isEnabled()).thenReturn(true);
              when(userDetails.isAccountNonLocked()).thenReturn(true);

              UserInfoExtractor.UserInfo userInfo =
                      UserInfoExtractor.UserInfo.builder()
                              .user(user)
                              .genderStr(user.getGender().name())
                              .build();
              when(userInfoExtractor.extractUserInfo(username)).thenReturn(userInfo);

              // when
              AuthenticatedUserInfo result = authInfoService.getCurrentUserInfo();

              // then
              assertThat(result).isNotNull();
              assertThat(result.getUsername()).isEqualTo(username);
              assertThat(result.getEmail()).isEqualTo("test@example.com");
              assertThat(result.getNickname()).isEqualTo("테스트사용자");
              assertThat(result.getGender()).isEqualTo("FEMALE");
              assertThat(result.getAuthorities()).containsExactly("ROLE_USER");
              assertThat(result.getExpiresAt()).isEqualTo("N/A");
              assertThat(result.getRemainingTime()).isEqualTo(-1L);
          }

          @Test
          @DisplayName("인증되지 않은 사용자 예외 발생")
          void getCurrentUserInfo_NotAuthenticated_ThrowsException() {
              // given
              when(securityContext.getAuthentication()).thenReturn(null);

              // when & then
              assertThatThrownBy(() -> authInfoService.getCurrentUserInfo())
                      .isInstanceOf(BusinessException.class)
                      .hasMessageContaining("인증되지 않은 사용자입니다");
          }

          @Test
          @DisplayName("익명 사용자 예외 발생")
          void getCurrentUserInfo_AnonymousUser_ThrowsException() {
              // given
              Authentication authentication =
                      new UsernamePasswordAuthenticationToken(
                              "anonymousUser", "password", Collections.emptyList());
              when(securityContext.getAuthentication()).thenReturn(authentication);

              // when & then
              assertThatThrownBy(() -> authInfoService.getCurrentUserInfo())
                      .isInstanceOf(BusinessException.class)
                      .hasMessageContaining("익명 사용자는 정보를 조회할 수 없습니다");
          }

          @Test
          @DisplayName("관리자 권한 사용자 정보 조회")
          void getCurrentUserInfo_AdminUser() {
              // given
              String username = "2";
              Long userId = 2L;

              User user =
                      User.builder()
                              .userId(userId)
                              .email("admin@example.com")
                              .nickname("관리자")
                              .role(User.Role.ROLE_ADMIN)
                              .build();

              Authentication authentication =
                      new UsernamePasswordAuthenticationToken(
                              username,
                              "password",
                              Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN")));

              when(securityContext.getAuthentication()).thenReturn(authentication);
              when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
              when(userDetails.isEnabled()).thenReturn(true);
              when(userDetails.isAccountNonLocked()).thenReturn(true);

              UserInfoExtractor.UserInfo userInfo =
                      UserInfoExtractor.UserInfo.builder()
                              .user(user)
                              .genderStr(null) // Admin user might not have gender
                              .build();
              when(userInfoExtractor.extractUserInfo(username)).thenReturn(userInfo);

              // when
              AuthenticatedUserInfo result = authInfoService.getCurrentUserInfo();

              // then
              assertThat(result.getAuthorities()).containsExactly("ROLE_ADMIN");
          }
      }

      @Nested
      @DisplayName("토큰 유효성 검증")
      class ValidateTokenInfoTest {

          @Test
          @DisplayName("유효한 토큰 검증")
          void validateTokenInfo_ValidToken() {
              // given
              String token = "valid.jwt.token";
              when(jwtUtil.validateToken(token)).thenReturn(true);
              when(jwtUtil.isTokenExpired(token)).thenReturn(false);
              when(jwtUtil.isTokenBlacklisted(token)).thenReturn(false);
              when(jwtUtil.getRemainingTime(token)).thenReturn(3600000L);

              // when
              AuthInfoServiceInterface.TokenValidationInfo result =
                      authInfoService.validateTokenInfo(token);

              // then
              assertThat(result.isValid()).isTrue();
              assertThat(result.isExpired()).isFalse();
              assertThat(result.isBlacklisted()).isFalse();
              assertThat(result.getRemainingTime()).isEqualTo(3600L);
          }

          @Test
          @DisplayName("만료된 토큰 검증")
          void validateTokenInfo_ExpiredToken() {
              // given
              String token = "expired.jwt.token";
              when(jwtUtil.validateToken(token)).thenReturn(false);
              when(jwtUtil.isTokenExpired(token)).thenReturn(true);
              when(jwtUtil.isTokenBlacklisted(token)).thenReturn(false);
              when(jwtUtil.getRemainingTime(token)).thenReturn(0L);

              // when
              AuthInfoServiceInterface.TokenValidationInfo result =
                      authInfoService.validateTokenInfo(token);

              // then
              assertThat(result.isValid()).isFalse();
              assertThat(result.isExpired()).isTrue();
              assertThat(result.isBlacklisted()).isFalse();
              assertThat(result.getRemainingTime()).isEqualTo(0L);
          }

          @Test
          @DisplayName("블랙리스트된 토큰 검증")
          void validateTokenInfo_BlacklistedToken() {
              // given
              String token = "blacklisted.jwt.token";
              when(jwtUtil.validateToken(token)).thenReturn(false);
              when(jwtUtil.isTokenExpired(token)).thenReturn(false);
              when(jwtUtil.isTokenBlacklisted(token)).thenReturn(true);
              when(jwtUtil.getRemainingTime(token)).thenReturn(1800000L);

              // when
              AuthInfoServiceInterface.TokenValidationInfo result =
                      authInfoService.validateTokenInfo(token);

              // then
              assertThat(result.isValid()).isFalse();
              assertThat(result.isExpired()).isFalse();
              assertThat(result.isBlacklisted()).isTrue();
              assertThat(result.getRemainingTime()).isEqualTo(1800L);
          }
      }

      @Nested
      @DisplayName("Helper 메서드")
      class HelperMethodsTest {

          @Test
          @DisplayName("Provider 감지 - 카카오")
          void detectProvider_Kakao() {
              // given
              String token = "valid.jwt.token";
              String username = "123456789"; // 숫자로만 구성

              when(jwtUtil.validateToken(token)).thenReturn(true);
              when(jwtUtil.extractUsername(token)).thenReturn(username);

              // 예외가 발생하기 전에 호출되는 메서드들
              JwtUtil.TokenInfo tokenInfo = new JwtUtil.TokenInfo();
              tokenInfo.setIssuedAt(Date.from(Instant.now().minusSeconds(3600)));
              tokenInfo.setExpiresAt(Date.from(Instant.now().plusSeconds(3600)));
              when(jwtUtil.getTokenInfo(token)).thenReturn(tokenInfo);
              when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);

              UserInfoExtractor.UserInfo userInfo =
                      UserInfoExtractor.UserInfo.builder().user(null).genderStr(null).build();
              when(userInfoExtractor.extractUserInfo(username)).thenReturn(userInfo);

              // when & then - 사용자 정보가 없을 때 예외 발생 예상
              assertThatThrownBy(() -> authInfoService.getUserInfoFromToken(token))
                      .isInstanceOf(BusinessException.class)
                      .hasMessageContaining("사용자 정보를 찾을 수 없습니다");
          }

          @Test
          @DisplayName("Provider 감지 - 로컬")
          void detectProvider_Local() {
              // given
              String token = "valid.jwt.token";
              String username = "user@example.com";

              when(jwtUtil.validateToken(token)).thenReturn(true);
              when(jwtUtil.extractUsername(token)).thenReturn(username);

              // 예외가 발생하기 전에 호출되는 메서드들
              JwtUtil.TokenInfo tokenInfo = new JwtUtil.TokenInfo();
              tokenInfo.setIssuedAt(Date.from(Instant.now().minusSeconds(3600)));
              tokenInfo.setExpiresAt(Date.from(Instant.now().plusSeconds(3600)));
              when(jwtUtil.getTokenInfo(token)).thenReturn(tokenInfo);
              when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);

              UserInfoExtractor.UserInfo userInfo =
                      UserInfoExtractor.UserInfo.builder().user(null).genderStr(null).build();
              when(userInfoExtractor.extractUserInfo(username)).thenReturn(userInfo);

              // when & then - 사용자 정보가 없을 때 예외 발생 예상
              assertThatThrownBy(() -> authInfoService.getUserInfoFromToken(token))
                      .isInstanceOf(BusinessException.class)
                      .hasMessageContaining("사용자 정보를 찾을 수 없습니다");
          }
      }
}
