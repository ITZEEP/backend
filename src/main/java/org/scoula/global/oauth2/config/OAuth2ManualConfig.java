package org.scoula.global.oauth2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * OAuth2 수동 처리 설정
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@Configuration
public class OAuth2ManualConfig {

      @Bean
      public RestTemplate restTemplate() {
          return new RestTemplate();
      }
}
