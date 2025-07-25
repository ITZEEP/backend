package org.scoula.global.auth.service;

import java.util.Optional;

import org.scoula.domain.user.service.UserServiceInterface;
import org.scoula.domain.user.vo.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * 사용자 정의 인증 서비스
 *
 * <p>Spring Security의 UserDetailsService를 구현하여 사용자 인증 정보를 제공합니다. 현재는 기본 사용자로 동작합니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class CustomUserDetailsService implements UserDetailsService {

      private final UserServiceInterface userService;

      @Override
      public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
          log.info("이메일로 사용자 로드: {}", email);

          // 데이터베이스에서 사용자 조회
          Optional<User> userOptional = userService.findByEmail(email);

          if (userOptional.isEmpty()) {
              log.warn("사용자를 찾을 수 없음: {}", email);
              throw new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email);
          }

          User user = userOptional.get();
          log.info(
                  "사용자 로드 성공 - ID: {}, Email: {}, Role: {}",
                  user.getUserId(),
                  user.getEmail(),
                  user.getRole());

          // 사용자 권한 설정
          String authority = user.getRole().name();

          return new org.scoula.global.auth.dto.CustomUserDetails(
                  user.getUserId(),
                  user.getEmail(),
                  user.getPassword() != null ? user.getPassword() : "{noop}oauth2",
                  authority);
      }
}
