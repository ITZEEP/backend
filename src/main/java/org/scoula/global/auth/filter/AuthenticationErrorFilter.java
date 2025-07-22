package org.scoula.global.auth.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.scoula.global.common.util.JsonResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;

@Component
public class AuthenticationErrorFilter extends OncePerRequestFilter {

      @Override
      protected void doFilterInternal(
              HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
              throws ServletException, IOException {

          try {
              filterChain.doFilter(request, response);
          } catch (ExpiredJwtException e) {
              // JWT 만료 예외만 처리
              JsonResponse.sendError(response, HttpStatus.UNAUTHORIZED, "토큰의 유효시간이 지났습니다.");
          } catch (UnsupportedJwtException | MalformedJwtException | SignatureException e) {
              // JWT 관련 예외만 처리
              JsonResponse.sendError(response, HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.");
          }
          // 다른 예외는 그대로 전파하여 Spring의 기본 예외 처리기가 처리하도록 함
      }
}
