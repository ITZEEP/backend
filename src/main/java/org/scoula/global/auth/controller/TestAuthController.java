package org.scoula.global.auth.controller;

import java.util.HashMap;
import java.util.Map;

import org.scoula.global.auth.jwt.JwtUtil;
import org.scoula.global.common.dto.ApiResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/** 개발 환경에서만 사용하는 테스트용 인증 컨트롤러 실제 운영 환경에서는 활성화되지 않음 */
@RestController
@RequestMapping("/api/test/auth")
@RequiredArgsConstructor
@Log4j2
@Profile({"local", "dev"}) // local과 dev 프로파일에서만 활성화
public class TestAuthController {

      private final JwtUtil jwtUtil;

      /**
       * 테스트용 JWT 토큰 생성
       *
       * <p>주의: 현재 JwtUtil은 email만을 subject로 사용하므로, userId와 role 정보는 토큰에 포함되지 않습니다. 테스트 목적으로 email을
       * "user1@example.com" 형식으로 사용합니다.
       *
       * @param userId 사용자 ID (기본값: 1) - email에 포함됨
       * @param email 이메일 (기본값: test@example.com)
       * @return JWT 토큰
       */
      @PostMapping("/generate-token")
      public ResponseEntity<ApiResponse<Map<String, String>>> generateTestToken(
              @RequestParam(defaultValue = "1") Long userId,
              @RequestParam(defaultValue = "test@example.com") String email,
              @RequestParam(defaultValue = "ROLE_USER") String role) {

          log.warn("테스트용 토큰 생성 요청 - userId: {}, email: {}, role: {}", userId, email, role);

          // 테스트를 위해 userId를 email에 포함
          // 예: user1@example.com (userId=1인 경우)
          String testEmail = "user" + userId + "@example.com";

          // JWT 토큰 생성
          String accessToken = jwtUtil.generateAccessToken(testEmail);
          String refreshToken = jwtUtil.generateRefreshToken(testEmail);

          Map<String, String> tokens = new HashMap<>();
          tokens.put("accessToken", accessToken);
          tokens.put("refreshToken", refreshToken);
          tokens.put("tokenType", "Bearer");
          tokens.put("expiresIn", String.valueOf(jwtUtil.getAccessTokenExpiration())); // 초 단위
          tokens.put("email", testEmail);
          tokens.put("userId", userId.toString());
          tokens.put("role", role);

          log.info("테스트용 토큰 생성 완료 - email: {}", testEmail);

          return ResponseEntity.ok(ApiResponse.success(tokens));
      }

      /**
       * 토큰 검증
       *
       * @param token JWT 토큰
       * @return 토큰 정보
       */
      @PostMapping("/validate-token")
      public ResponseEntity<ApiResponse<Map<String, Object>>> validateToken(
              @RequestParam String token) {

          try {
              boolean isValid = jwtUtil.validateToken(token);

              Map<String, Object> result = new HashMap<>();
              result.put("valid", isValid);

              if (isValid) {
                  // JwtUtil은 username(실제로는 email)만 추출 가능
                  String email = jwtUtil.extractUsername(token);

                  result.put("email", email);
                  // userId는 현재 토큰에 포함되지 않음
              }

              return ResponseEntity.ok(ApiResponse.success(result));
          } catch (Exception e) {
              log.error("토큰 검증 실패", e);
              return ResponseEntity.ok(ApiResponse.error("토큰 검증 실패: " + e.getMessage()));
          }
      }
}
