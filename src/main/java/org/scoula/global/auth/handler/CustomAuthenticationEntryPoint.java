package org.scoula.global.auth.handler;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.scoula.global.common.util.JsonResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

      @Override
      public void commence(
              HttpServletRequest request,
              HttpServletResponse response,
              AuthenticationException authException)
              throws IOException, ServletException {

          log.error("Unauthorized error: {}", authException.getMessage());
          JsonResponse.sendError(response, HttpStatus.UNAUTHORIZED, authException.getMessage());
      }
}
