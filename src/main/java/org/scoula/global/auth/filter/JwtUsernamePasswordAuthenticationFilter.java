package org.scoula.global.auth.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.scoula.global.auth.dto.LoginDTO;
import org.scoula.global.auth.handler.LoginFailureHandler;
import org.scoula.global.auth.handler.LoginSuccessHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class JwtUsernamePasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

      public JwtUsernamePasswordAuthenticationFilter(
              AuthenticationManager authenticationManager,
              LoginSuccessHandler loginSuccessHandler,
              LoginFailureHandler loginFailureHandler) {

          super(authenticationManager);

          setFilterProcessesUrl("/api/auth/login");
          setAuthenticationSuccessHandler(loginSuccessHandler);
          setAuthenticationFailureHandler(loginFailureHandler);
      }

      @Override
      public Authentication attemptAuthentication(
              HttpServletRequest request, HttpServletResponse response)
              throws AuthenticationException {

          LoginDTO login = LoginDTO.of(request);
          log.info("로그인 시도 - 사용자명: {}", login.getUsername());

          UsernamePasswordAuthenticationToken authenticationToken =
                  new UsernamePasswordAuthenticationToken(login.getUsername(), login.getPassword());

          return getAuthenticationManager().authenticate(authenticationToken);
      }
}
