package org.scoula.global.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scoula.domain.user.service.UserServiceInterface;
import org.scoula.domain.user.vo.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * CustomUserDetailsService 단위 테스트
 *
 * <p>사용자 인증 정보 로드 서비스를 테스트합니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService 단위 테스트")
class CustomUserDetailsServiceTest {

      @Mock private UserServiceInterface userService;

      private CustomUserDetailsService customUserDetailsService;

      @BeforeEach
      void setUp() {
          customUserDetailsService = new CustomUserDetailsService(userService);
      }

      @Test
      @DisplayName("유효한 이메일로 사용자 정보를 로드할 수 있는지 확인")
      void loadUserByUsername_WithValidEmail_ShouldReturnUserDetails() {
          // given
          String email = "test@example.com";
          User mockUser =
                  User.builder()
                          .userId(1L)
                          .email(email)
                          .nickname("testUser")
                          .password("hashedPassword")
                          .role(User.Role.ROLE_USER)
                          .build();

          when(userService.findByEmail(email)).thenReturn(Optional.of(mockUser));

          // when
          UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

          // then
          assertThat(userDetails).isNotNull();
          assertThat(userDetails.getUsername()).isEqualTo(email);
          assertThat(userDetails.getPassword()).isEqualTo("hashedPassword");
          assertThat(userDetails.isEnabled()).isTrue();
          assertThat(userDetails.isAccountNonExpired()).isTrue();
          assertThat(userDetails.isAccountNonLocked()).isTrue();
          assertThat(userDetails.isCredentialsNonExpired()).isTrue();

          verify(userService).findByEmail(email);
      }

      @Test
      @DisplayName("로드된 사용자가 ROLE_USER 권한을 가지는지 확인")
      void loadUserByUsername_ShouldReturnUserWithUserRole() {
          // given
          String email = "test@example.com";
          User mockUser =
                  User.builder()
                          .userId(1L)
                          .email(email)
                          .nickname("testUser")
                          .password("hashedPassword")
                          .role(User.Role.ROLE_USER)
                          .build();

          when(userService.findByEmail(email)).thenReturn(Optional.of(mockUser));

          // when
          UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

          // then
          Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
          assertThat(authorities).isNotNull();
          assertThat(authorities).hasSize(1);
          assertThat(authorities).extracting("authority").contains("ROLE_USER");
      }

      @Test
      @DisplayName("존재하지 않는 이메일로 사용자 로드 시 예외가 발생하는지 확인")
      void loadUserByUsername_WithNonExistentEmail_ShouldThrowException() {
          // given
          String email = "nonexistent@example.com";
          when(userService.findByEmail(email)).thenReturn(Optional.empty());

          // when & then
          assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(email))
                  .isInstanceOf(UsernameNotFoundException.class)
                  .hasMessageContaining("사용자를 찾을 수 없습니다: " + email);

          verify(userService).findByEmail(email);
      }

      @Test
      @DisplayName("OAuth2 사용자(패스워드 없음)에 대한 처리 확인")
      void loadUserByUsername_WithOAuth2User_ShouldReturnUserWithNoopPassword() {
          // given
          String email = "oauth2user@example.com";
          User mockUser =
                  User.builder()
                          .userId(1L)
                          .email(email)
                          .nickname("oauthUser")
                          .password(null) // OAuth2 사용자는 패스워드가 없을 수 있음
                          .role(User.Role.ROLE_USER)
                          .build();

          when(userService.findByEmail(email)).thenReturn(Optional.of(mockUser));

          // when
          UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

          // then
          assertThat(userDetails).isNotNull();
          assertThat(userDetails.getUsername()).isEqualTo(email);
          assertThat(userDetails.getPassword()).isEqualTo("{noop}oauth2");
          assertThat(userDetails.getAuthorities()).extracting("authority").contains("ROLE_USER");

          verify(userService).findByEmail(email);
      }

      @Test
      @DisplayName("ADMIN 권한 사용자에 대한 처리 확인")
      void loadUserByUsername_WithAdminUser_ShouldReturnUserWithAdminRole() {
          // given
          String email = "admin@example.com";
          User mockUser =
                  User.builder()
                          .userId(1L)
                          .email(email)
                          .nickname("admin")
                          .password("hashedPassword")
                          .role(User.Role.ROLE_ADMIN)
                          .build();

          when(userService.findByEmail(email)).thenReturn(Optional.of(mockUser));

          // when
          UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

          // then
          Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
          assertThat(authorities).isNotNull();
          assertThat(authorities).hasSize(1);
          assertThat(authorities).extracting("authority").contains("ROLE_ADMIN");

          verify(userService).findByEmail(email);
      }
}
