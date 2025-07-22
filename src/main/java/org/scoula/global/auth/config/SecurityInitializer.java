package org.scoula.global.auth.config;

import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;

public class SecurityInitializer extends AbstractSecurityWebApplicationInitializer {
      // CharacterEncodingFilter는 SecurityConfig에서 이미 등록되므로 여기서는 제거
}
