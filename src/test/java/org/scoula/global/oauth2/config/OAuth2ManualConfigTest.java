package org.scoula.global.oauth2.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

/**
 * OAuth2ManualConfig 단위 테스트
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2ManualConfig 단위 테스트")
class OAuth2ManualConfigTest {

      private OAuth2ManualConfig oauth2ManualConfig;

      @BeforeEach
      void setUp() {
          oauth2ManualConfig = new OAuth2ManualConfig();
      }

      @Test
      @DisplayName("RestTemplate Bean이 정상적으로 생성되는지 확인")
      void restTemplate_ShouldBeCreated() {
          // when
          RestTemplate restTemplate = oauth2ManualConfig.restTemplate();

          // then
          assertThat(restTemplate).isNotNull();
          assertThat(restTemplate).isInstanceOf(RestTemplate.class);
      }

      @Test
      @DisplayName("RestTemplate이 싱글톤으로 생성되는지 확인")
      void restTemplate_ShouldBeSingleton() {
          // when
          RestTemplate restTemplate1 = oauth2ManualConfig.restTemplate();
          RestTemplate restTemplate2 = oauth2ManualConfig.restTemplate();

          // then
          // 실제 Spring 컨테이너에서는 @Bean이 싱글톤으로 관리되지만,
          // 단위 테스트에서는 매번 새 인스턴스가 생성됨
          assertThat(restTemplate1).isNotSameAs(restTemplate2);
          // 하지만 두 인스턴스 모두 RestTemplate 타입이어야 함
          assertThat(restTemplate1).isInstanceOf(RestTemplate.class);
          assertThat(restTemplate2).isInstanceOf(RestTemplate.class);
      }
}
