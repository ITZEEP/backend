package org.scoula.global.oauth2.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * OAuth2AuthorizationController 단위 테스트
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2AuthorizationController 단위 테스트")
class OAuth2AuthorizationControllerTest {

      @InjectMocks private OAuth2AuthorizationControllerImpl oauth2AuthorizationController;

      private MockMvc mockMvc;

      private static final String KAKAO_CLIENT_ID = "test-client-id";
      private static final String KAKAO_REDIRECT_URI = "http://localhost:3000/oauth/callback/kakao";

      @BeforeEach
      void setUp() {
          mockMvc = MockMvcBuilders.standaloneSetup(oauth2AuthorizationController).build();

          // @Value 필드 주입
          ReflectionTestUtils.setField(
                  oauth2AuthorizationController, "kakaoClientId", KAKAO_CLIENT_ID);
          ReflectionTestUtils.setField(
                  oauth2AuthorizationController, "kakaoRedirectUri", KAKAO_REDIRECT_URI);
      }

      @Nested
      @DisplayName("OAuth2 Authorization 처리")
      class AuthorizeTest {

          @Test
          @DisplayName("카카오 로그인 페이지로 리다이렉트 성공")
          void authorize_Kakao_Success() throws Exception {
              // when
              MvcResult result =
                      mockMvc.perform(get("/oauth2/authorization/kakao"))
                              .andExpect(status().is3xxRedirection())
                              .andReturn();

              // then
              String redirectUrl = result.getResponse().getRedirectedUrl();
              assertThat(redirectUrl).isNotNull();
              assertThat(redirectUrl).startsWith("https://kauth.kakao.com/oauth/authorize");
              assertThat(redirectUrl).contains("client_id=" + KAKAO_CLIENT_ID);
              assertThat(redirectUrl).contains("redirect_uri=");
              assertThat(redirectUrl).contains("response_type=code");
              assertThat(redirectUrl)
                      .contains(
                              "scope=profile_nickname,account_email,profile_image,gender,age_range");
              assertThat(redirectUrl).contains("state=");
          }

          @Test
          @DisplayName("카카오 리다이렉트 URL에 모든 필수 파라미터 포함 확인")
          void authorize_Kakao_ContainsAllRequiredParams() throws Exception {
              // when
              MvcResult result =
                      mockMvc.perform(get("/oauth2/authorization/kakao"))
                              .andExpect(status().is3xxRedirection())
                              .andReturn();

              // then
              String redirectUrl = result.getResponse().getRedirectedUrl();

              // URL 파라미터 파싱
              String[] urlParts = redirectUrl.split("\\?");
              assertThat(urlParts).hasSize(2);

              String queryString = urlParts[1];
              assertThat(queryString).isNotEmpty();

              // 필수 파라미터 확인
              assertThat(queryString).contains("client_id=");
              assertThat(queryString).contains("redirect_uri=");
              assertThat(queryString).contains("response_type=code");
              assertThat(queryString).contains("scope=");
              assertThat(queryString).contains("state=");
          }

          @Test
          @DisplayName("State 파라미터가 매번 다른 값으로 생성되는지 확인")
          void authorize_Kakao_GeneratesUniqueState() throws Exception {
              // when
              MvcResult result1 =
                      mockMvc.perform(get("/oauth2/authorization/kakao"))
                              .andExpect(status().is3xxRedirection())
                              .andReturn();

              MvcResult result2 =
                      mockMvc.perform(get("/oauth2/authorization/kakao"))
                              .andExpect(status().is3xxRedirection())
                              .andReturn();

              // then
              String redirectUrl1 = result1.getResponse().getRedirectedUrl();
              String redirectUrl2 = result2.getResponse().getRedirectedUrl();

              // state 값 추출
              String state1 = extractStateFromUrl(redirectUrl1);
              String state2 = extractStateFromUrl(redirectUrl2);

              assertThat(state1).isNotNull();
              assertThat(state2).isNotNull();
              assertThat(state1).isNotEqualTo(state2);
          }

          @Test
          @DisplayName("지원하지 않는 OAuth2 provider 요청 시 404 에러")
          void authorize_UnsupportedProvider_Returns404() throws Exception {
              // when & then
              mockMvc.perform(get("/oauth2/authorization/google")).andExpect(status().isNotFound());
          }

          @Test
          @DisplayName("지원하지 않는 provider 에러 메시지 확인")
          void authorize_UnsupportedProvider_ErrorMessage() throws Exception {
              // when
              MvcResult result =
                      mockMvc.perform(get("/oauth2/authorization/facebook"))
                              .andExpect(status().isNotFound())
                              .andReturn();

              // then
              String errorMessage = result.getResponse().getErrorMessage();
              assertThat(errorMessage).contains("지원하지 않는 OAuth2 provider: facebook");
          }

          @Test
          @DisplayName("Scope에 5개 권한이 모두 포함되는지 확인")
          void authorize_Kakao_ContainsAllScopes() throws Exception {
              // when
              MvcResult result =
                      mockMvc.perform(get("/oauth2/authorization/kakao"))
                              .andExpect(status().is3xxRedirection())
                              .andReturn();

              // then
              String redirectUrl = result.getResponse().getRedirectedUrl();
              assertThat(redirectUrl)
                      .contains(
                              "scope=profile_nickname,account_email,profile_image,gender,age_range");
          }

          @Test
          @DisplayName("Redirect URI가 URL 인코딩되는지 확인")
          void authorize_Kakao_EncodesRedirectUri() throws Exception {
              // when
              MvcResult result =
                      mockMvc.perform(get("/oauth2/authorization/kakao"))
                              .andExpect(status().is3xxRedirection())
                              .andReturn();

              // then
              String redirectUrl = result.getResponse().getRedirectedUrl();
              // URL 인코딩된 redirect_uri가 포함되어 있는지 확인
              // 특히 ':' -> '%3A', '/' -> '%2F'로 인코딩되는지 확인
              assertThat(redirectUrl).contains("redirect_uri=");
              assertThat(redirectUrl).contains("%3A"); // ':' encoded
              assertThat(redirectUrl).contains("%2F"); // '/' encoded
          }
      }

      // Helper method
      private String extractStateFromUrl(String url) {
          String[] params = url.split("[?&]");
          for (String param : params) {
              if (param.startsWith("state=")) {
                  return param.substring(6);
              }
          }
          return null;
      }
}
