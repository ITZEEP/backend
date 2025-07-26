package org.scoula.global.auth.dto;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 사용자 인증 정보를 담는 CustomUserDetails */
@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {
      private final Long userId;
      private final String email;
      private final String password;
      private final String role;

      @Override
      public Collection<? extends GrantedAuthority> getAuthorities() {
          return Collections.singletonList(new SimpleGrantedAuthority(role));
      }

      @Override
      public String getPassword() {
          return password;
      }

      @Override
      public String getUsername() {
          return email;
      }

      @Override
      public boolean isAccountNonExpired() {
          return true;
      }

      @Override
      public boolean isAccountNonLocked() {
          return true;
      }

      @Override
      public boolean isCredentialsNonExpired() {
          return true;
      }

      @Override
      public boolean isEnabled() {
          return true;
      }
}
