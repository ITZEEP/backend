package org.scoula.global.auth.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.scoula.global.auth.jwt.JwtUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

      public static final String AUTHORIZATION_HEADER = "Authorization";
      public static final String BEARER_PREFIX = "Bearer ";

      private final JwtUtil jwtUtil;
      private final UserDetailsService userDetailsService;

      private Authentication getAuthentication(String token) {
          String username = jwtUtil.getUsername(token);
          UserDetails principal = userDetailsService.loadUserByUsername(username);

          return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
      }

      @Override
      protected void doFilterInternal(
              HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
              throws ServletException, IOException {

          String requestURI = request.getRequestURI();
          log.debug("JWT 필터 처리 시작 - URI: {}", requestURI);

          String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
          log.debug("Authorization 헤더: {}", bearerToken);

          if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
              String token = bearerToken.substring(BEARER_PREFIX.length());
              log.info("JWT Token 추출 - 토큰 길이: {}", token.length());

              if (jwtUtil.validateToken(token)) {
                  try {
                      String username = jwtUtil.getUsername(token);
                      log.info("토큰에서 추출한 사용자명: {}", username);

                      Authentication authentication = getAuthentication(token);
                      SecurityContextHolder.getContext().setAuthentication(authentication);
                      log.info(
                              "인증 설정 완료 - 사용자: {}, 권한: {}, URI: {}",
                              authentication.getName(),
                              authentication.getAuthorities(),
                              requestURI);
                  } catch (Exception e) {
                      log.error("인증 처리 중 오류 발생 - URI: {}", requestURI, e);
                      SecurityContextHolder.clearContext();
                  }
              } else {
                  log.warn("유효하지 않은 JWT 토큰 - URI: {}", requestURI);
              }
          } else {
              log.debug("Authorization 헤더 없음 또는 Bearer 토큰이 아님 - URI: {}", requestURI);
          }

          // 인증 상태 확인
          Authentication auth = SecurityContextHolder.getContext().getAuthentication();
          if (auth != null && auth.isAuthenticated()) {
              log.debug("필터 통과 후 인증 상태: 인증됨 - 사용자: {}", auth.getName());
          } else {
              log.debug("필터 통과 후 인증 상태: 인증되지 않음");
          }

          filterChain.doFilter(request, response);
      }
}
