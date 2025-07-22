package org.scoula.global.auth.controller;

import org.scoula.global.auth.dto.AuthenticatedUserInfo;
import org.scoula.global.auth.service.AuthInfoServiceInterface;
import org.scoula.global.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * 인증 정보 컨트롤러 구현체
 *
 * <p>로그인한 사용자의 인증 정보를 제공하는 API 엔드포인트를 구현합니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/auth/info")
@RequiredArgsConstructor
@Log4j2
public class AuthInfoControllerImpl implements AuthInfoController {

      private final AuthInfoServiceInterface authInfoService;

      /** {@inheritDoc} */
      @Override
      @GetMapping("/current")
      public ResponseEntity<ApiResponse<AuthenticatedUserInfo>> getCurrentUser() {
          try {
              AuthenticatedUserInfo userInfo = authInfoService.getCurrentUserInfo();
              return ResponseEntity.ok(ApiResponse.success(userInfo, "사용자 정보 조회 성공"));
          } catch (Exception e) {
              log.error("사용자 정보 조회 실패", e);
              return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                      .body(ApiResponse.error("AUTH_ERROR", e.getMessage()));
          }
      }
}
