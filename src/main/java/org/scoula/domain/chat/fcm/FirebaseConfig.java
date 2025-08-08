package org.scoula.domain.chat.fcm;

import java.io.InputStream;

import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

@Configuration
public class FirebaseConfig {

      @PostConstruct
      public void initialize() {
          try {
              InputStream serviceAccount =
                      getClass()
                              .getClassLoader()
                              .getResourceAsStream("firebase-service-account.json");

              FirebaseOptions options =
                      FirebaseOptions.builder()
                              .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                              .build();

              if (FirebaseApp.getApps().isEmpty()) {
                  FirebaseApp.initializeApp(options);
              }
          } catch (Exception e) {
              e.printStackTrace();
          }
      }
}
