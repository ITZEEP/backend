package org.scoula.global.auth.handler;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.scoula.global.common.util.JsonResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class LoginFailureHandler implements AuthenticationFailureHandler {

      @Override
      public void onAuthenticationFailure(
              HttpServletRequest request,
              HttpServletResponse response,
              AuthenticationException exception)
              throws IOException, ServletException {

          log.warn("로그인 실패: {}", exception.getMessage());
          JsonResponse.sendError(
                  response, HttpStatus.UNAUTHORIZED, "로그인에 실패했습니다. 아이디와 비밀번호를 확인해주세요.");
      }
}
