package org.scoula.global.oauth2.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.log4j.Log4j2;

/**
 * OAuth2 Authorization 엔드포인트 컨트롤러 구현체
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/oauth2")
@Log4j2
public class OAuth2AuthorizationControllerImpl implements OAuth2AuthorizationControllerInterface {

      @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
      private String kakaoClientId;

      // 프론트엔드 콜백 URL 사용
      @Value("${oauth2.frontend.callback-url:http://localhost:3000/oauth/callback/kakao}")
      private String kakaoRedirectUri;

      /** {@inheritDoc} */
      @Override
      @GetMapping("/authorization/{registrationId}")
      public void authorize(@PathVariable String registrationId, HttpServletResponse response)
              throws Exception {
          log.info("OAuth2 authorization 요청: registrationId={}", registrationId);

          if ("kakao".equals(registrationId)) {
              redirectToKakao(response);
          } else {
              response.sendError(
                      HttpServletResponse.SC_NOT_FOUND, "지원하지 않는 OAuth2 provider: " + registrationId);
          }
      }

      private void redirectToKakao(HttpServletResponse response) throws Exception {
          String state = UUID.randomUUID().toString();

          String authUrl =
                  "https://kauth.kakao.com/oauth/authorize"
                          + "?client_id="
                          + kakaoClientId
                          + "&redirect_uri="
                          + URLEncoder.encode(kakaoRedirectUri, StandardCharsets.UTF_8)
                          + "&response_type=code"
                          + "&scope=profile_nickname,account_email,profile_image,gender,age_range"
                          + "&state="
                          + state;

          log.info("카카오 로그인 페이지로 리다이렉트: {}", authUrl);
          response.sendRedirect(authUrl);
      }
}
