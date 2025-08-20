package org.scoula.domain.chat.fcm;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

@Configuration
public class FirebaseConfig {

      @Value("${firebase.credentials}")
      private String firebaseCredentials;

      @PostConstruct
      public void initialize() {
          try {
              // Firebase 자격증명이 비어있거나 유효하지 않은 경우 건너뛰기
              if (firebaseCredentials == null
                      || firebaseCredentials.trim().isEmpty()
                      || firebaseCredentials.equals("${firebase.credentials}")) {
                  System.out.println("Firebase 설정이 없습니다. FCM 기능을 사용할 수 없습니다.");
                  return;
              }

              // JSON 문자열에서 이스케이프된 개행 문자를 실제 개행 문자로 변환
              String processedCredentials = firebaseCredentials.replace("\\n", "\n");

              InputStream serviceAccount =
                      new ByteArrayInputStream(processedCredentials.getBytes(StandardCharsets.UTF_8));

              FirebaseOptions options =
                      FirebaseOptions.builder()
                              .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                              .build();

              if (FirebaseApp.getApps().isEmpty()) {
                  FirebaseApp.initializeApp(options);
                  System.out.println("Firebase 초기화 완료");
              }
          } catch (Exception e) {
              System.err.println("Firebase 초기화 실패: " + e.getMessage());
              e.printStackTrace();
          }
      }
}
