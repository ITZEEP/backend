package org.scoula.global.websocket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

      static {
          System.err.println("🚨🚨🚨 WebSocketConfig 클래스 로딩됨!");
      }

      public WebSocketConfig() {
          System.err.println("🚨🚨🚨 WebSocketConfig 생성자 호출됨!");
      }

      @Override
      public void configureMessageBroker(MessageBrokerRegistry config) {
          System.err.println("🚨🚨🚨 MessageBroker 설정 시작");
          config.enableSimpleBroker("/topic");
          config.setApplicationDestinationPrefixes("/app");
          System.err.println("🚨🚨🚨 MessageBroker 설정 완료");
      }

      @Override
      public void registerStompEndpoints(StompEndpointRegistry registry) {
          System.err.println("🚨🚨🚨 STOMP 엔드포인트 등록 시작");
          registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
          System.err.println("🚨🚨🚨 STOMP 엔드포인트 등록 완료");
      }
}
