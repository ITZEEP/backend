package org.scoula.global.websocket.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@DisplayName("WebSocket 설정 단위 테스트")
class WebSocketConfigTest {

      private WebSocketConfig webSocketConfig;

      @BeforeEach
      void setUp() {
          webSocketConfig = new WebSocketConfig();
      }

      @Test
      @DisplayName("WebSocketConfig가 정상적으로 생성되는지 확인")
      void webSocketConfig_ShouldBeCreated() {
          // then
          assertThat(webSocketConfig).isNotNull();
          assertThat(webSocketConfig).isInstanceOf(WebSocketMessageBrokerConfigurer.class);
      }

      @Test
      @DisplayName("WebSocket 설정 클래스가 올바른 인터페이스를 구현하는지 확인")
      void webSocketConfig_ShouldImplementCorrectInterface() {
          // then
          assertThat(webSocketConfig).isInstanceOf(WebSocketMessageBrokerConfigurer.class);
      }

      @Test
      @DisplayName("메시지 브로커 설정 메서드가 존재하는지 확인")
      void configureMessageBroker_ShouldExist() throws NoSuchMethodException {
          // when
          var method =
                  WebSocketConfig.class.getMethod(
                          "configureMessageBroker", MessageBrokerRegistry.class);

          // then
          assertThat(method).isNotNull();
          assertThat(method.getReturnType()).isEqualTo(void.class);
      }

      @Test
      @DisplayName("STOMP 엔드포인트 등록 메서드가 존재하는지 확인")
      void registerStompEndpoints_ShouldExist() throws NoSuchMethodException {
          // when
          var method =
                  WebSocketConfig.class.getMethod(
                          "registerStompEndpoints", StompEndpointRegistry.class);

          // then
          assertThat(method).isNotNull();
          assertThat(method.getReturnType()).isEqualTo(void.class);
      }

      @Test
      @DisplayName("WebSocket 설정 클래스에 올바른 어노테이션이 적용되어 있는지 확인")
      void webSocketConfig_ShouldHaveCorrectAnnotations() {
          // when
          Class<WebSocketConfig> configClass = WebSocketConfig.class;

          // then
          assertThat(
                          configClass.isAnnotationPresent(
                                  org.springframework.context.annotation.Configuration.class))
                  .isTrue();
          assertThat(
                          configClass.isAnnotationPresent(
                                  org.springframework.web.socket.config.annotation
                                          .EnableWebSocketMessageBroker.class))
                  .isTrue();
      }
}
