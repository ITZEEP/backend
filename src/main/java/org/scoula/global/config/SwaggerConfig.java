package org.scoula.global.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/** SpringFox Swagger 설정 클래스 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {

      private final String API_NAME = "ITZeep API";
      private final String API_VERSION = "1.0.0";
      private final String API_DESCRIPTION =
              "ITZeep API 문서\n\n"
                      + "**웹소켓 엔드포인트:**\n"
                      + "- 연결: ws://localhost:8080/ws\n"
                      + "- 공개 채팅: /app/chat.sendMessage → /topic/public\n"
                      + "- 방 채팅: /app/chat/{roomId}/sendMessage → /topic/room/{roomId}\n"
                      + "- 개인 메시지: /app/chat.sendPrivate → /user/queue/private\n"
                      + "- 타이핑 상태: /app/chat/{roomId}/typing → /topic/room/{roomId}/typing";

      @Bean
      public Docket api() {
          return new Docket(DocumentationType.SWAGGER_2)
                  .host("localhost:8080") // 명시적 호스트 설정
                  .select()
                  .apis(RequestHandlerSelectors.basePackage("org.scoula"))
                  .paths(PathSelectors.any())
                  .build()
                  .apiInfo(apiInfo())
                  .securityContexts(Arrays.asList(securityContext()))
                  .securitySchemes(Arrays.asList(apiKey()));
      }

      private ApiInfo apiInfo() {
          return new ApiInfoBuilder()
                  .title(API_NAME)
                  .version(API_VERSION)
                  .description(API_DESCRIPTION)
                  .build();
      }

      private ApiKey apiKey() {
          return new ApiKey("JWT", "Authorization", "header");
      }

      private SecurityContext securityContext() {
          return SecurityContext.builder().securityReferences(defaultAuth()).build();
      }

      private List<SecurityReference> defaultAuth() {
          AuthorizationScope authorizationScope =
                  new AuthorizationScope("global", "accessEverything");
          AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
          authorizationScopes[0] = authorizationScope;
          return Arrays.asList(new SecurityReference("JWT", authorizationScopes));
      }
}
