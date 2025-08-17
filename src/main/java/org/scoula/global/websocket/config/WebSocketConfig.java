package org.scoula.global.websocket.config;

import java.util.Collections;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import lombok.extern.log4j.Log4j2;

@Log4j2
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
          config.enableSimpleBroker("/topic", "/queue");
          config.setApplicationDestinationPrefixes("/app");
          config.setUserDestinationPrefix("/user");
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

      @Override
      public void configureClientInboundChannel(ChannelRegistration registration) {
          registration.interceptors(
                  new ChannelInterceptor() {
                      @Override
                      public Message<?> preSend(Message<?> message, MessageChannel channel) {
                          StompHeaderAccessor accessor =
                                  MessageHeaderAccessor.getAccessor(
                                          message, StompHeaderAccessor.class);

                          if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                              log.info("🔐 WebSocket 연결 시도 - 헤더 확인");

                              // 헤더에서 인증 정보 확인
                              String authHeader = accessor.getFirstNativeHeader("Authorization");
                              String userId = accessor.getFirstNativeHeader("X-User-Id");

                              log.info(
                                      "🔍 받은 헤더 - Authorization: {}, X-User-Id: {}",
                                      authHeader != null ? "있음" : "없음",
                                      userId);

                              // JWT 토큰에서 사용자 ID 추출
                              if (authHeader != null && authHeader.startsWith("Bearer ")) {
                                  try {
                                      String token = authHeader.substring(7);
                                      // 간단한 JWT 파싱 (실제로는 JwtUtil 사용 권장)
                                      String[] parts = token.split("\\.");
                                      if (parts.length == 3) {
                                          String payload =
                                                  new String(
                                                          java.util.Base64.getDecoder()
                                                                  .decode(parts[1]));
                                          log.info("🔍 JWT 페이로드: {}", payload);

                                          // 페이로드에서 sub (사용자 이메일) 추출
                                          if (payload.contains("\"sub\"")) {
                                              String[] subParts = payload.split("\"sub\":\"");
                                              if (subParts.length > 1) {
                                                  String userEmail = subParts[1].split("\"")[0];
                                                  log.info("🔍 JWT에서 추출한 사용자: {}", userEmail);

                                                  // Principal 설정
                                                  UsernamePasswordAuthenticationToken auth =
                                                          new UsernamePasswordAuthenticationToken(
                                                                  userEmail,
                                                                  null,
                                                                  Collections.emptyList());
                                                  accessor.setUser(auth);
                                                  log.info(
                                                          "✅ WebSocket 인증 성공 (JWT) - User: {}",
                                                          userEmail);
                                              }
                                          }
                                      }
                                  } catch (Exception e) {
                                      log.error("❌ JWT 토큰 파싱 실패: {}", e.getMessage());
                                  }
                              }

                              // X-User-Id 헤더가 있으면 사용 (백업)
                              if (accessor.getUser() == null && userId != null && !userId.isEmpty()) {
                                  UsernamePasswordAuthenticationToken auth =
                                          new UsernamePasswordAuthenticationToken(
                                                  userId, null, Collections.emptyList());
                                  accessor.setUser(auth);
                                  log.info("✅ WebSocket 인증 성공 (User-Id) - UserId: {}", userId);
                              }

                              log.info(
                                      "🔐 최종 Principal 상태: {}",
                                      accessor.getUser() != null
                                              ? accessor.getUser().getName()
                                              : "null");
                          }

                          return message;
                      }
                  });
      }
}
