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
          // Nginx가 CORS 헤더를 추가하지만, Spring WebSocket도 Origin 검증이 필요
          // setAllowedOriginPatterns를 사용하여 Spring의 Origin 검증은 허용하되
          // 실제 CORS 헤더는 Nginx에서 관리
          registry.addEndpoint("/ws")
                  .setAllowedOriginPatterns(
                          "http://localhost:5173",
                          "https://localhost:5173",
                          "https://itzeep.ariogi.kr",
                          "https://www.itzeep.ariogi.kr",
                          "http://itzeep.ariogi.kr",
                          "http://www.itzeep.ariogi.kr")
                  .withSockJS();
          System.err.println("🚨🚨🚨 STOMP 엔드포인트 등록 완료");
      }
}
