package org.scoula.global.auth.jwt;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;

import org.scoula.global.common.constant.Constants;
import org.scoula.global.common.exception.BusinessException;
import org.scoula.global.common.exception.CommonErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

/** JWT 유틸리티 클래스 토큰 생성, 검증, 추출 기능을 제공하며 안전한 JWT 작업을 지원합니다 */
@Component
@Log4j2
public class JwtUtil {

      @Value("${jwt.secret}")
      private String secret;

      @Value("${jwt.access-token-validity-in-seconds}") // 1시간 (초)
      private Long expiration;

      @Value("${jwt.refresh-token-validity-in-seconds}") // 7일 (초)
      private Long refreshExpiration;

      @Value("${jwt.issuer:issuer}")
      private String issuer;

      // 로그아웃 기능을 위한 토큰 블랙리스트
      private final Map<String, LocalDateTime> blacklistedTokens = new ConcurrentHashMap<>();

      @Data
      public static class TokenInfo {
          private String token;
          private String username;
          private Date issuedAt;
          private Date expiresAt;
          private boolean expired;
      }

      @PostConstruct
      public void init() {
          // 필드 초기화 검증
          if (!StringUtils.hasText(secret)) {
              throw new IllegalStateException("JWT secret이 설정되지 않았습니다");
          }
          if (expiration == null || expiration <= 0) {
              throw new IllegalStateException("JWT 액세스 토큰 만료 시간이 유효하지 않습니다");
          }
          if (refreshExpiration == null || refreshExpiration <= 0) {
              throw new IllegalStateException("JWT 리프레시 토큰 만료 시간이 유효하지 않습니다");
          }
          if (!StringUtils.hasText(issuer)) {
              throw new IllegalStateException("JWT 발급자가 설정되지 않았습니다");
          }

          log.info(
                  "JWT 설정 초기화 완료 - 발급자: {}, 액세스 토큰 만료: {}초, 리프레시 토큰 만료: {}초",
                  issuer,
                  expiration,
                  refreshExpiration);
      }

      private SecretKey getSigningKey() {
          byte[] keyBytes;
          try {
              // Try to decode as Base64 first
              keyBytes = Decoders.BASE64.decode(secret);
          } catch (Exception e) {
              // If not Base64 encoded, use the secret as-is
              log.warn("JWT secret is not Base64 encoded, using as plain text");
              keyBytes = secret.getBytes();
          }

          // Ensure the key is at least 256 bits (32 bytes) for HS256
          if (keyBytes.length < 32) {
              log.warn("JWT secret is too short, padding to 256 bits");
              byte[] paddedKey = new byte[32];
              System.arraycopy(keyBytes, 0, paddedKey, 0, keyBytes.length);
              keyBytes = paddedKey;
          }

          return Keys.hmacShaKeyFor(keyBytes);
      }

      /**
       * 주어진 사용자명에 대한 액세스 토큰 생성
       *
       * @param username 토큰을 생성할 사용자명
       * @return JWT 액세스 토큰
       * @throws BusinessException 사용자명이 유효하지 않은 경우
       */
      public String generateToken(String username) {
          return generateToken(username, expiration);
      }

      /**
       * 주어진 사용자명에 대한 액세스 토큰 생성
       *
       * @param email 사용자 이메일
       * @return JWT 액세스 토큰
       * @throws BusinessException 이메일이 유효하지 않은 경우
       */
      public String generateAccessToken(String email) {
          return generateToken(email, expiration);
      }

      /**
       * 주어진 사용자명에 대한 리프레시 토큰 생성
       *
       * @param username 토큰을 생성할 사용자명
       * @return JWT 리프레시 토큰
       * @throws BusinessException 사용자명이 유효하지 않은 경우
       */
      public String generateRefreshToken(String username) {
          return generateToken(username, refreshExpiration);
      }

      /**
       * 액세스 토큰의 만료 시간을 초 단위로 반환
       *
       * @return 만료 시간 (초)
       */
      public Long getAccessTokenExpiration() {
          return expiration;
      }

      /**
       * 리프레시 토큰의 만료 시간을 초 단위로 반환
       *
       * @return 만료 시간 (초)
       */
      public Long getRefreshTokenExpiration() {
          return refreshExpiration;
      }

      /**
       * 사용자 정의 만료 시간으로 토큰 생성
       *
       * @param username 토큰을 생성할 사용자명
       * @param tokenExpiration 사용자 정의 만료 시간 (초)
       * @return JWT 토큰
       * @throws BusinessException 매개변수가 유효하지 않은 경우
       */
      public String generateToken(String username, Long tokenExpiration) {
          validateUsername(username);
          validateExpiration(tokenExpiration);

          Date now = new Date();
          Date expiryDate = new Date(now.getTime() + (tokenExpiration * 1000));

          try {
              String token =
                      Jwts.builder()
                              .setSubject(username)
                              .setIssuer(issuer)
                              .setIssuedAt(now)
                              .setExpiration(expiryDate)
                              .signWith(getSigningKey())
                              .compact();

              log.debug("사용자에 대한 JWT 토큰 생성: {}", username);
              return token;
          } catch (Exception e) {
              log.error("사용자에 대한 JWT 토큰 생성 실패: {}", username, e);
              throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, "토큰 생성에 실패했습니다", e);
          }
      }

      /**
       * JWT 토큰에서 사용자명 추출
       *
       * @param token JWT 토큰
       * @return 토큰에서 추출한 사용자명
       * @throws BusinessException 토큰이 유효하지 않은 경우
       */
      public String getUsername(String token) {
          return extractUsername(token);
      }

      /**
       * JWT 토큰에서 사용자명 추출
       *
       * @param token JWT 토큰
       * @return 토큰에서 추출한 사용자명
       * @throws BusinessException 토큰이 유효하지 않은 경우
       */
      public String extractUsername(String token) {
          validateTokenFormat(token);

          try {
              return getClaims(token).getSubject();
          } catch (Exception e) {
              log.error("토큰에서 사용자명 추출 실패", e);
              throw new BusinessException(CommonErrorCode.INVALID_TOKEN, "토큰에서 사용자명 추출에 실패했습니다", e);
          }
      }

      /**
       * 토큰의 상세 정보 가져오기
       *
       * @param token JWT 토큰
       * @return 토큰 세부 정보가 포함된 TokenInfo 객체
       * @throws BusinessException 토큰이 유효하지 않은 경우
       */
      public TokenInfo getTokenInfo(String token) {
          validateTokenFormat(token);

          try {
              Claims claims = getClaims(token);
              TokenInfo tokenInfo = new TokenInfo();
              tokenInfo.setToken(token);
              tokenInfo.setUsername(claims.getSubject());
              tokenInfo.setIssuedAt(claims.getIssuedAt());
              tokenInfo.setExpiresAt(claims.getExpiration());
              tokenInfo.setExpired(isTokenExpired(token));

              return tokenInfo;
          } catch (Exception e) {
              log.error("토큰 정보 가져오기 실패", e);
              throw new BusinessException(CommonErrorCode.INVALID_TOKEN, "토큰 정보 가져오기에 실패했습니다", e);
          }
      }

      /**
       * JWT 토큰 검증
       *
       * @param token 검증할 JWT 토큰
       * @return 토큰이 유효하면 true, 그렇지 않으면 false
       */
      public boolean validateToken(String token) {
          if (!StringUtils.hasText(token)) {
              log.warn("빈 토큰 검증 시도");
              return false;
          }

          if (isTokenBlacklisted(token)) {
              log.warn("블랙리스트된 토큰 검증 시도");
              return false;
          }

          try {
              Claims claims = getClaims(token);

              // 추가 검증
              if (!issuer.equals(claims.getIssuer())) {
                  log.warn("잘못된 토큰 발급자: {}", claims.getIssuer());
                  return false;
              }

              return true;
          } catch (MalformedJwtException ex) {
              log.warn("잘못된 JWT 토큰 형식");
          } catch (ExpiredJwtException ex) {
              log.warn("JWT 토큰이 만료되었습니다");
          } catch (UnsupportedJwtException ex) {
              log.warn("지원되지 않는 JWT 토큰");
          } catch (IllegalArgumentException ex) {
              log.warn("JWT 클레임 문자열이 비어있습니다");
          } catch (Exception ex) {
              log.error("토큰 검증 중 예상치 못한 오류", ex);
          }
          return false;
      }

      /**
       * 토큰 검증 및 유효하지 않으면 예외 발생
       *
       * @param token 검증할 JWT 토큰
       * @throws BusinessException 토큰이 유효하지 않은 경우
       */
      public void validateTokenOrThrow(String token) {
          if (!validateToken(token)) {
              throw new BusinessException(CommonErrorCode.INVALID_TOKEN, "유효하지 않은 JWT 토큰입니다");
          }
      }

      // 개인 헬퍼 메서드

      private Claims getClaims(String token) {
          try {
              return Jwts.parser()
                      .setSigningKey(getSigningKey())
                      .requireIssuer(issuer)
                      .build()
                      .parseClaimsJws(token)
                      .getBody();
          } catch (Exception e) {
              log.debug("JWT 클레임 파싱 실패", e);
              throw e;
          }
      }

      // 검증 메서드

      private void validateUsername(String username) {
          if (!StringUtils.hasText(username)) {
              throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE, "사용자명이 비어있을 수 없습니다");
          }

          if (username.length() > 255) {
              throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE, "사용자명이 너무 깁니다");
          }
      }

      private void validateExpiration(Long expiration) {
          if (expiration == null || expiration <= 0) {
              throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE, "만료 시간은 양수여야 합니다");
          }

          // 최대 30일 (초 단위)
          if (expiration > Constants.Jwt.MAX_EXPIRATION_DAYS * 24 * 60 * 60) {
              throw new BusinessException(
                      CommonErrorCode.INVALID_INPUT_VALUE, "만료 시간이 너무 깁니다 (최대 30일)");
          }
      }

      private void validateTokenFormat(String token) {
          if (!StringUtils.hasText(token)) {
              throw new BusinessException(CommonErrorCode.INVALID_TOKEN, "토큰이 비어있을 수 없습니다");
          }

          // 기본 JWT 형식 확인 (점으로 구분된 3개 부분)
          String[] parts = token.split("\\.");
          if (parts.length != 3) {
              throw new BusinessException(CommonErrorCode.INVALID_TOKEN, "잘못된 토큰 형식입니다");
          }
      }

      /**
       * Check if token is expired
       *
       * @param token JWT token
       * @return true if token is expired
       */
      public boolean isTokenExpired(String token) {
          try {
              Date expiration = getClaims(token).getExpiration();
              return expiration.before(new Date());
          } catch (ExpiredJwtException e) {
              return true;
          } catch (Exception e) {
              log.warn("Error checking token expiration", e);
              return true; // Assume expired if we can't verify
          }
      }

      /**
       * Get remaining time until token expires
       *
       * @param token JWT token
       * @return remaining time in milliseconds, -1 if expired or invalid
       */
      public long getRemainingTime(String token) {
          try {
              Date expiration = getClaims(token).getExpiration();
              long remaining = expiration.getTime() - System.currentTimeMillis();
              return Math.max(0, remaining);
          } catch (Exception e) {
              log.warn("Error getting remaining token time", e);
              return -1;
          }
      }

      /**
       * Blacklist a token (for logout functionality)
       *
       * @param token JWT token to blacklist
       */
      public void blacklistToken(String token) {
          if (StringUtils.hasText(token)) {
              try {
                  Date expiration = getClaims(token).getExpiration();
                  LocalDateTime expirationTime =
                          expiration.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                  blacklistedTokens.put(token, expirationTime);
                  log.debug("Token blacklisted successfully");
              } catch (Exception e) {
                  log.warn("Failed to blacklist token", e);
              }
          }
      }

      /**
       * Check if token is blacklisted
       *
       * @param token JWT token to check
       * @return true if token is blacklisted
       */
      public boolean isTokenBlacklisted(String token) {
          if (!StringUtils.hasText(token)) {
              return false;
          }

          LocalDateTime expirationTime = blacklistedTokens.get(token);
          if (expirationTime == null) {
              return false;
          }

          // Remove expired blacklisted tokens
          if (LocalDateTime.now().isAfter(expirationTime)) {
              blacklistedTokens.remove(token);
              return false;
          }

          return true;
      }

      /** Clean up expired blacklisted tokens */
      public void cleanupExpiredBlacklistedTokens() {
          LocalDateTime now = LocalDateTime.now();
          blacklistedTokens.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
          log.debug("Cleaned up expired blacklisted tokens");
      }
}
