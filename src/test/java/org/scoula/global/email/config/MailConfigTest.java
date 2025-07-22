package org.scoula.global.email.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("메일 설정 단위 테스트")
class MailConfigTest {

      private MailConfig mailConfig;

      @BeforeEach
      void setUp() {
          mailConfig = new MailConfig();
          ReflectionTestUtils.setField(mailConfig, "mailHost", "smtp.gmail.com");
          ReflectionTestUtils.setField(mailConfig, "mailPort", 587);
          ReflectionTestUtils.setField(mailConfig, "mailUsername", "test@gmail.com");
          ReflectionTestUtils.setField(mailConfig, "mailPassword", "password");
          ReflectionTestUtils.setField(mailConfig, "mailAuth", true);
          ReflectionTestUtils.setField(mailConfig, "mailStartTls", true);
          ReflectionTestUtils.setField(mailConfig, "mailDebug", false);
      }

      @Test
      @DisplayName("JavaMailSender Bean이 정상적으로 생성되는지 확인")
      void javaMailSender_ShouldBeCreatedProperly() {
          // when
          JavaMailSender javaMailSender = mailConfig.javaMailSender();

          // then
          assertThat(javaMailSender).isNotNull();
          assertThat(javaMailSender).isInstanceOf(JavaMailSenderImpl.class);
      }

      @Test
      @DisplayName("메일 서버 설정이 정상적으로 적용되는지 확인")
      void mailServerConfiguration_ShouldBeAppliedProperly() {
          // when
          JavaMailSenderImpl mailSender = (JavaMailSenderImpl) mailConfig.javaMailSender();

          // then
          assertThat(mailSender.getHost()).isEqualTo("smtp.gmail.com");
          assertThat(mailSender.getPort()).isEqualTo(587);
          assertThat(mailSender.getUsername()).isEqualTo("test@gmail.com");
          assertThat(mailSender.getPassword()).isEqualTo("password");
      }

      @Test
      @DisplayName("메일 프로퍼티 설정이 정상적으로 적용되는지 확인")
      void mailProperties_ShouldBeConfiguredProperly() {
          // when
          JavaMailSenderImpl mailSender = (JavaMailSenderImpl) mailConfig.javaMailSender();
          Properties props = mailSender.getJavaMailProperties();

          // then
          assertThat(props).isNotNull();
          assertThat(props.getProperty("mail.transport.protocol")).isEqualTo("smtp");
          assertThat(props.get("mail.smtp.auth")).isEqualTo(true);
          assertThat(props.get("mail.smtp.starttls.enable")).isEqualTo(true);
          assertThat(props.get("mail.debug")).isEqualTo(false);
          assertThat(props.getProperty("mail.mime.charset")).isEqualTo("UTF-8");
      }

      @Test
      @DisplayName("UTF-8 인코딩 설정이 올바르게 구성되는지 확인")
      void utf8EncodingConfiguration_ShouldBeProperlyConfigured() {
          // when
          JavaMailSenderImpl mailSender = (JavaMailSenderImpl) mailConfig.javaMailSender();
          Properties props = mailSender.getJavaMailProperties();

          // then
          assertThat(props.getProperty("mail.mime.charset")).isEqualTo("UTF-8");
      }
}
