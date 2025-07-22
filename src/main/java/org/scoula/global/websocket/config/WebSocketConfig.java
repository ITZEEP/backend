package org.scoula.global.websocket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

      @Override
      public void configureMessageBroker(MessageBrokerRegistry config) {
          // 클라이언트로 메시지를 보낼 때 사용할 prefix
          config.enableSimpleBroker("/topic", "/queue");
          // 클라이언트에서 메시지를 보낼 때 사용할 prefix
          config.setApplicationDestinationPrefixes("/app");
          // 특정 사용자에게 메시지를 보낼 때 사용할 prefix
          config.setUserDestinationPrefix("/user");
      }

      @Override
      public void registerStompEndpoints(StompEndpointRegistry registry) {
          // WebSocket 연결 엔드포인트
          registry.addEndpoint("/ws").setAllowedOrigins("*").withSockJS();

          // SockJS를 사용하지 않는 순수 WebSocket 엔드포인트
          registry.addEndpoint("/ws-raw").setAllowedOrigins("*");
      }
}
