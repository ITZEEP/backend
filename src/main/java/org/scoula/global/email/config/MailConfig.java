package org.scoula.global.email.config;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/** 메일 설정 클래스 */
@Configuration
public class MailConfig {

      @Value("${spring.mail.host:smtp.gmail.com}")
      private String mailHost;

      @Value("${spring.mail.port:587}")
      private int mailPort;

      @Value("${spring.mail.username:}")
      private String mailUsername;

      @Value("${spring.mail.password:}")
      private String mailPassword;

      @Value("${spring.mail.properties.mail.smtp.auth:true}")
      private boolean mailAuth;

      @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}")
      private boolean mailStartTls;

      @Value("${spring.mail.debug:false}")
      private boolean mailDebug;

      @Bean
      public JavaMailSender javaMailSender() {
          JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
          mailSender.setHost(mailHost);
          mailSender.setPort(mailPort);
          mailSender.setUsername(mailUsername);
          mailSender.setPassword(mailPassword);

          Properties props = mailSender.getJavaMailProperties();
          props.put("mail.transport.protocol", "smtp");
          props.put("mail.smtp.auth", mailAuth);
          props.put("mail.smtp.starttls.enable", mailStartTls);
          props.put("mail.debug", mailDebug);

          // UTF-8 인코딩 설정
          props.put("mail.mime.charset", "UTF-8");

          return mailSender;
      }
}
