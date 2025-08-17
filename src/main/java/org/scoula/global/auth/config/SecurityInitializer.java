package org.scoula.global.auth.config;

import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;

// Spring Security 필터 체인을 서블릿 컨테이너에 등록
// WebConfig에서 SecurityConfig를 Root Config로 등록하고
// 이 클래스가 Spring Security 필터들을 실제로 활성화함
public class SecurityInitializer extends AbstractSecurityWebApplicationInitializer {
      // Spring Security 필터 체인 자동 등록
      // CharacterEncodingFilter는 SecurityConfig에서 처리
}
