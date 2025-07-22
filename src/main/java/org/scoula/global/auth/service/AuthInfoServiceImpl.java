package org.scoula.global.auth.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.scoula.domain.user.mapper.UserMapper;
import org.scoula.global.auth.dto.AuthenticatedUserInfo;
import org.scoula.global.auth.jwt.JwtUtil;
import org.scoula.global.auth.util.UserInfoExtractor;
import org.scoula.global.common.exception.BusinessException;
import org.scoula.global.common.exception.CommonErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * 인증 정보 서비스
 *
 * <p>JWT 토큰과 Spring Security를 통해 인증된 사용자의 정보를 제공합니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class AuthInfoServiceImpl implements AuthInfoServiceInterface {

      private final JwtUtil jwtUtil;
      private final CustomUserDetailsService userDetailsService;
      private final UserMapper userMapper;
      private final UserInfoExtractor userInfoExtractor;

      private static final DateTimeFormatter DATETIME_FORMATTER =
              DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

      /** {@inheritDoc} */
      @Override
      public AuthenticatedUserInfo getUserInfoFromToken(String token) {
          log.info("JWT 토큰으로부터 사용자 정보 조회 시작");

          try {
              // 토큰 검증
              if (!jwtUtil.validateToken(token)) {
                  throw new BusinessException(CommonErrorCode.INVALID_TOKEN, "유효하지 않은 토큰입니다");
              }

              // 토큰에서 사용자명 추출
              String username = jwtUtil.extractUsername(token);
              log.info("토큰에서 추출된 사용자명: {}", username);

              // 토큰 정보 가져오기
              JwtUtil.TokenInfo tokenInfo = jwtUtil.getTokenInfo(token);

              // 사용자 세부 정보 조회
              UserDetails userDetails = userDetailsService.loadUserByUsername(username);

              // 권한 목록 변환
              List<String> authorities =
                      userDetails.getAuthorities().stream()
                              .map(GrantedAuthority::getAuthority)
                              .collect(Collectors.toList());

              // 시간 포맷팅
              String issuedAt = formatDate(tokenInfo.getIssuedAt().toInstant());
              String expiresAt = formatDate(tokenInfo.getExpiresAt().toInstant());
              Long remainingTime = jwtUtil.getRemainingTime(token) / 1000; // 초 단위로 변환

              // DB에서 실제 사용자 정보 조회
              UserInfoExtractor.UserInfo userInfo = userInfoExtractor.extractUserInfo(username);

              // 사용자 정보가 없는 경우 에러 처리
              if (userInfo.getUser() == null) {
                  log.error("사용자 정보를 찾을 수 없음: {}", username);
                  throw new BusinessException(
                          CommonErrorCode.ENTITY_NOT_FOUND, "사용자 정보를 찾을 수 없습니다: " + username);
              }

              return AuthenticatedUserInfo.builder()
                      .username(username)
                      .userId(String.valueOf(userInfo.getUser().getUserId())) // 실제 DB의 user ID
                      .email(
                              userInfo.getEmail() != null
                                      ? userInfo.getEmail()
                                      : extractEmailFromUsername(username))
                      .nickname(
                              userInfo.getNickname() != null
                                      ? userInfo.getNickname()
                                      : extractNicknameFromUsername(username))
                      .profileImageUrl(userInfo.getProfileImageUrl())
                      .gender(userInfo.getGenderStr())
                      .authorities(authorities)
                      .issuedAt(issuedAt)
                      .expiresAt(expiresAt)
                      .remainingTime(remainingTime)
                      .provider(detectProvider(username))
                      .enabled(userDetails.isEnabled())
                      .locked(!userDetails.isAccountNonLocked())
                      .build();

          } catch (Exception e) {
              log.error("JWT 토큰으로부터 사용자 정보 조회 실패", e);
              if (e instanceof BusinessException) {
                  throw e;
              }
              throw new BusinessException(
                      CommonErrorCode.INTERNAL_SERVER_ERROR, "사용자 정보 조회에 실패했습니다: " + e.getMessage());
          }
      }

      /** {@inheritDoc} */
      @Override
      public AuthenticatedUserInfo getCurrentUserInfo() {
          log.info("현재 인증된 사용자 정보 조회 시작");

          Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

          if (authentication == null || !authentication.isAuthenticated()) {
              throw new BusinessException(CommonErrorCode.AUTHENTICATION_FAILED, "인증되지 않은 사용자입니다");
          }

          String username = authentication.getName();
          if ("anonymousUser".equals(username)) {
              throw new BusinessException(
                      CommonErrorCode.AUTHENTICATION_FAILED, "익명 사용자는 정보를 조회할 수 없습니다");
          }

          try {
              // 사용자 세부 정보 조회
              UserDetails userDetails = userDetailsService.loadUserByUsername(username);

              // 권한 목록 변환
              List<String> authorities =
                      authentication.getAuthorities().stream()
                              .map(GrantedAuthority::getAuthority)
                              .collect(Collectors.toList());

              // DB에서 실제 사용자 정보 조회
              UserInfoExtractor.UserInfo userInfo = userInfoExtractor.extractUserInfo(username);

              // 사용자 정보가 없는 경우 에러 처리
              if (userInfo.getUser() == null) {
                  log.error("사용자 정보를 찾을 수 없음: {}", username);
                  throw new BusinessException(
                          CommonErrorCode.ENTITY_NOT_FOUND, "사용자 정보를 찾을 수 없습니다: " + username);
              }

              return AuthenticatedUserInfo.builder()
                      .username(username)
                      .userId(String.valueOf(userInfo.getUser().getUserId())) // 실제 DB의 user ID
                      .email(
                              userInfo.getEmail() != null
                                      ? userInfo.getEmail()
                                      : extractEmailFromUsername(username))
                      .nickname(
                              userInfo.getNickname() != null
                                      ? userInfo.getNickname()
                                      : extractNicknameFromUsername(username))
                      .profileImageUrl(userInfo.getProfileImageUrl())
                      .gender(userInfo.getGenderStr())
                      .authorities(authorities)
                      .issuedAt(formatDate(Instant.now()))
                      .expiresAt("N/A") // SecurityContext에서는 정확한 만료시간 알 수 없음
                      .remainingTime(-1L) // SecurityContext에서는 정확한 남은시간 알 수 없음
                      .provider(detectProvider(username))
                      .enabled(userDetails.isEnabled())
                      .locked(!userDetails.isAccountNonLocked())
                      .build();

          } catch (Exception e) {
              log.error("현재 사용자 정보 조회 실패", e);
              if (e instanceof BusinessException) {
                  throw e;
              }
              throw new BusinessException(
                      CommonErrorCode.INTERNAL_SERVER_ERROR, "사용자 정보 조회에 실패했습니다: " + e.getMessage());
          }
      }

      /** {@inheritDoc} */
      @Override
      public TokenValidationInfo validateTokenInfo(String token) {
          log.info("토큰 유효성 검증 시작");

          boolean isValid = jwtUtil.validateToken(token);
          boolean isExpired = jwtUtil.isTokenExpired(token);
          boolean isBlacklisted = jwtUtil.isTokenBlacklisted(token);
          long remainingTime = jwtUtil.getRemainingTime(token);

          return new TokenValidationInfo(isValid, isExpired, isBlacklisted, remainingTime / 1000);
      }

      // Helper methods

      private String formatDate(Instant instant) {
          LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
          return dateTime.format(DATETIME_FORMATTER);
      }

      private String extractEmailFromUsername(String username) {
          // OAuth2 사용자의 경우 이메일이 없을 수 있음
          // 추후 실제 사용자 데이터에서 조회하도록 개선 가능
          if (username.contains("@")) {
              return username;
          }
          return null;
      }

      private String extractNicknameFromUsername(String username) {
          // OAuth2 사용자의 경우 실제 닉네임 조회 필요
          // 추후 실제 사용자 데이터에서 조회하도록 개선 가능
          return username;
      }

      private String detectProvider(String username) {
          // OAuth2 provider 감지 로직
          // 이메일 형식이면 local 사용자로 간주
          if (username.contains("@")) {
              return "local";
          }
          // 숫자로만 구성된 경우 카카오로 추정
          if (username.matches("\\d+")) {
              return "kakao";
          }
          return "local";
      }
}
