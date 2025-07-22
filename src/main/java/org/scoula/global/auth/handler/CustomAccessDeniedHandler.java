package org.scoula.global.auth.handler;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.scoula.global.common.util.JsonResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

      @Override
      public void handle(
              HttpServletRequest request,
              HttpServletResponse response,
              AccessDeniedException accessDeniedException)
              throws IOException, ServletException {

          log.error("Access Denied: {}", accessDeniedException.getMessage());
          JsonResponse.sendError(response, HttpStatus.FORBIDDEN, "접근 권한이 없습니다. 관리자에게 문의하세요.");
      }
}
