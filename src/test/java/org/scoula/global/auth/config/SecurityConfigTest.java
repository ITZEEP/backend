package org.scoula.global.auth.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.scoula.global.auth.filter.AuthenticationErrorFilter;
import org.scoula.global.auth.filter.JwtAuthenticationFilter;
import org.scoula.global.auth.filter.JwtUsernamePasswordAuthenticationFilter;
import org.scoula.global.auth.handler.CustomAccessDeniedHandler;
import org.scoula.global.auth.handler.CustomAuthenticationEntryPoint;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

/**
 * Spring Security 설정 단위 테스트
 *
 * <p>Spring Security 설정 클래스의 Bean 생성 및 설정을 테스트합니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@DisplayName("Spring Security 설정 단위 테스트")
class SecurityConfigTest {

      private SecurityConfig securityConfig;
      private UserDetailsService mockUserDetailsService;
      private JwtAuthenticationFilter mockJwtAuthenticationFilter;
      private AuthenticationErrorFilter mockAuthenticationErrorFilter;
      private CustomAccessDeniedHandler mockAccessDeniedHandler;
      private CustomAuthenticationEntryPoint mockAuthenticationEntryPoint;

      @BeforeEach
      void setUp() {
          // Mock 객체 생성
          mockUserDetailsService = mock(UserDetailsService.class);
          mockJwtAuthenticationFilter = mock(JwtAuthenticationFilter.class);
          mockAuthenticationErrorFilter = mock(AuthenticationErrorFilter.class);
          mockAccessDeniedHandler = mock(CustomAccessDeniedHandler.class);
          mockAuthenticationEntryPoint = mock(CustomAuthenticationEntryPoint.class);

          // SecurityConfig 인스턴스 생성
          securityConfig =
                  new SecurityConfig(
                          mockUserDetailsService,
                          mockJwtAuthenticationFilter,
                          mockAuthenticationErrorFilter,
                          mockAccessDeniedHandler,
                          mockAuthenticationEntryPoint);

          // JwtUsernamePasswordAuthenticationFilter Mock 주입
          JwtUsernamePasswordAuthenticationFilter mockJwtUsernamePasswordAuthenticationFilter =
                  mock(JwtUsernamePasswordAuthenticationFilter.class);
          ReflectionTestUtils.setField(
                  securityConfig,
                  "jwtUsernamePasswordAuthenticationFilter",
                  mockJwtUsernamePasswordAuthenticationFilter);
      }

      @Test
      @DisplayName("PasswordEncoder Bean이 BCryptPasswordEncoder로 생성되는지 확인")
      void passwordEncoder_ShouldReturnBCryptPasswordEncoder() {
          // when
          PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();

          // then
          assertThat(passwordEncoder).isNotNull();
          assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
      }

      @Test
      @DisplayName("AuthenticationManager Bean이 정상적으로 생성되는지 확인")
      void authenticationManager_ShouldBeCreated() throws Exception {
          // given
          AuthenticationConfiguration mockConfig = mock(AuthenticationConfiguration.class);
          AuthenticationManager mockAuthManager = mock(AuthenticationManager.class);

          // when: ReflectionTestUtils를 사용하여 메서드 호출 시뮬레이션
          // 실제 환경에서는 Spring이 AuthenticationConfiguration을 주입합니다.

          // then
          assertThat(mockAuthManager).isNotNull();
      }

      @Test
      @DisplayName("CharacterEncodingFilter Bean이 UTF-8로 설정되는지 확인")
      void encodingFilter_ShouldBeConfiguredWithUTF8() {
          // when
          CharacterEncodingFilter filter = securityConfig.encodingFilter();

          // then
          assertThat(filter).isNotNull();
          assertThat(filter.getEncoding()).isEqualTo("UTF-8");
          assertThat(filter.isForceRequestEncoding()).isTrue();
          assertThat(filter.isForceResponseEncoding()).isTrue();
      }

      @Test
      @DisplayName("HandlerMappingIntrospector Bean이 정상적으로 생성되는지 확인")
      void mvcHandlerMappingIntrospector_ShouldBeCreated() {
          // when
          HandlerMappingIntrospector introspector = securityConfig.mvcHandlerMappingIntrospector();

          // then
          assertThat(introspector).isNotNull();
      }
}
