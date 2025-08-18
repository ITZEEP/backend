package org.scoula.global.auth.config;

import java.util.Arrays;

import org.scoula.global.auth.filter.AuthenticationErrorFilter;
import org.scoula.global.auth.filter.JwtAuthenticationFilter;
import org.scoula.global.auth.filter.JwtUsernamePasswordAuthenticationFilter;
import org.scoula.global.auth.handler.CustomAccessDeniedHandler;
import org.scoula.global.auth.handler.CustomAuthenticationEntryPoint;
import org.scoula.global.auth.handler.LoginFailureHandler;
import org.scoula.global.auth.handler.LoginSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Configuration
@EnableWebSecurity
@Log4j2
@ComponentScan(basePackages = {"org.scoula.global.auth"})
@RequiredArgsConstructor
public class SecurityConfig {

      private final UserDetailsService userDetailsService;

      private final JwtAuthenticationFilter jwtAuthenticationFilter;
      private final AuthenticationErrorFilter authenticationErrorFilter;

      private final CustomAccessDeniedHandler accessDeniedHandler;
      private final CustomAuthenticationEntryPoint authenticationEntryPoint;

      private final LoginSuccessHandler loginSuccessHandler;
      private final LoginFailureHandler loginFailureHandler;

      @Bean
      public PasswordEncoder passwordEncoder() {
          return new BCryptPasswordEncoder();
      }

      @Bean
      public CorsConfigurationSource corsConfigurationSource() {
          CorsConfiguration configuration = new CorsConfiguration();
          configuration.setAllowedOrigins(
                  Arrays.asList(
                          "http://localhost:5173",
                          "http://localhost:8080",
                          "https://itzeep.ariogi.kr",
                          "https://api.itzeep.ariogi.kr"));
          configuration.setAllowedMethods(
                  Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
          configuration.setAllowedHeaders(Arrays.asList("*"));
          configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition"));
          configuration.setAllowCredentials(true);
          configuration.setMaxAge(3600L);

          UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
          source.registerCorsConfiguration("/**", configuration);
          return source;
      }

      @Bean
      public CharacterEncodingFilter encodingFilter() {
          CharacterEncodingFilter filter = new CharacterEncodingFilter();
          filter.setEncoding("UTF-8");
          filter.setForceEncoding(true);
          return filter;
      }

      @Bean
      public HandlerMappingIntrospector mvcHandlerMappingIntrospector() {
          return new HandlerMappingIntrospector();
      }

      @Bean
      public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

          return http
                  // http 기본 설정
                  .httpBasic()
                  .disable()
                  .csrf()
                  .disable()
                  .cors() // CORS 활성화 (multipart/form-data 지원을 위해)
                  .and()
                  .formLogin()
                  .disable()
                  .sessionManagement()
                  .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                  .and()

                  // 접근 경로 제어 설정
                  .authorizeHttpRequests(
                          auth ->
                                  auth
                                          // OPTIONS 요청은 모두 허용
                                          .requestMatchers(
                                                  new AntPathRequestMatcher(
                                                          "/**", HttpMethod.OPTIONS.name()))
                                          .permitAll()
                                          // 공개 API
                                          .requestMatchers(
                                                  new AntPathRequestMatcher("/"), // 루트 경로
                                                  new AntPathRequestMatcher(
                                                          "/api", HttpMethod.GET.name()),
                                                  new AntPathRequestMatcher("/api/health"),
                                                  new AntPathRequestMatcher("/api/auth/login"),
                                                  new AntPathRequestMatcher("/api/auth/signup"),
                                                  new AntPathRequestMatcher("/api/auth/refresh"),
                                                  new AntPathRequestMatcher("/api/auth/oauth/**"),
                                                  new AntPathRequestMatcher(
                                                          "/oauth2/**"), // OAuth2 엔드포인트 추가
                                                  new AntPathRequestMatcher(
                                                          "/api/homes/**"), // 매물 조회는 인증 불필요
                                                  new AntPathRequestMatcher(
                                                          "/ws/**"), // WebSocket 엔드포인트
                                                  new AntPathRequestMatcher(
                                                          "/ws/info"), // WebSocket info
                                                  new AntPathRequestMatcher("/swagger-ui.html"),
                                                  new AntPathRequestMatcher("/swagger-ui/**"),
                                                  new AntPathRequestMatcher("/v2/api-docs"),
                                                  new AntPathRequestMatcher("/v2/api-docs/**"),
                                                  new AntPathRequestMatcher("/swagger-resources/**"),
                                                  new AntPathRequestMatcher("/webjars/**"))
                                          .permitAll()
                                          // 사기 위험도 분석 API는 인증 필요
                                          .requestMatchers(
                                                  new AntPathRequestMatcher("/api/fraud-risk/**"))
                                          .authenticated()
                                          // 나머지 모든 요청은 인증 필요
                                          .anyRequest()
                                          .authenticated())
                  // 필터 설정
                  .addFilterBefore(encodingFilter(), CsrfFilter.class)
                  .addFilterBefore(
                          authenticationErrorFilter, UsernamePasswordAuthenticationFilter.class)
                  .addFilterBefore(
                          jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                  .addFilterBefore(
                          jwtUsernamePasswordAuthenticationFilter(http),
                          UsernamePasswordAuthenticationFilter.class)

                  // 예외처리 설정
                  .exceptionHandling()
                  .authenticationEntryPoint(authenticationEntryPoint)
                  .accessDeniedHandler(accessDeniedHandler)
                  .and()
                  .build();
      }

      @Bean
      public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
          AuthenticationManagerBuilder authenticationManagerBuilder =
                  http.getSharedObject(AuthenticationManagerBuilder.class);

          return authenticationManagerBuilder
                  .userDetailsService(userDetailsService)
                  .passwordEncoder(passwordEncoder())
                  .and()
                  .build();
      }

      // 순환 참조 해결을 위해 메서드로 생성
      public JwtUsernamePasswordAuthenticationFilter jwtUsernamePasswordAuthenticationFilter(
              HttpSecurity http) throws Exception {
          return new JwtUsernamePasswordAuthenticationFilter(
                  authenticationManager(http), loginSuccessHandler, loginFailureHandler);
      }
}
