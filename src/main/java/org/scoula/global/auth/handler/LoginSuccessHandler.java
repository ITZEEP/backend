package org.scoula.global.auth.handler;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.scoula.global.auth.dto.AuthenticatedUserInfo;
import org.scoula.global.auth.jwt.JwtUtil;
import org.scoula.global.auth.service.AuthInfoServiceInterface;
import org.scoula.global.common.util.JsonResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

      private final JwtUtil jwtUtil;
      private final AuthInfoServiceInterface authInfoService;

      @Override
      public void onAuthenticationSuccess(
              HttpServletRequest request, HttpServletResponse response, Authentication authentication)
              throws IOException, ServletException {

          try {
              UserDetails userDetails = (UserDetails) authentication.getPrincipal();
              String username = userDetails.getUsername();

              // JWT 토큰 생성
              String token = jwtUtil.generateToken(username);

              // 사용자 정보 조회
              AuthenticatedUserInfo userInfo = authInfoService.getUserInfoFromToken(token);

              // 응답 데이터 생성
              AuthResult authResult = new AuthResult(token, userInfo);

              log.info("로그인 성공 - 사용자: {}", username);
              JsonResponse.sendSuccess(response, authResult);
          } catch (Exception e) {
              log.error("로그인 성공 처리 중 오류 발생", e);
              JsonResponse.sendError(
                      response, HttpStatus.INTERNAL_SERVER_ERROR, "로그인 처리 중 오류가 발생했습니다.");
          }
      }

      // 로그인 응답 DTO
      @lombok.Getter
      private static class AuthResult {
          private final String token;
          private final AuthenticatedUserInfo user;

          public AuthResult(String token, AuthenticatedUserInfo user) {
              this.token = token;
              this.user = user;
          }
      }
}
