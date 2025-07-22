package org.scoula.global.email.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scoula.global.common.exception.BusinessException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("이메일 서비스 단위 테스트")
class EmailServiceTest {

      @Mock private JavaMailSender javaMailSender;

      private EmailServiceInterface emailService;

      @BeforeEach
      void setUp() {
          emailService = new EmailServiceImpl(javaMailSender);

          // 테스트용 설정값 주입
          ReflectionTestUtils.setField(emailService, "fromAddress", "test@itzeep.com");
          ReflectionTestUtils.setField(emailService, "frontendUrl", "https://test.itzeep.com");
      }

      @Test
      @DisplayName("단순 이메일 전송 - 정상 전송 성공")
      void sendSimpleEmail_ShouldSendEmailSuccessfully() {
          // given
          String to = "recipient@test.com";
          String subject = "테스트 이메일";
          String text = "테스트 내용";

          doNothing().when(javaMailSender).send(any(SimpleMailMessage.class));

          // when & then (예외가 발생하지 않아야 함)
          emailService.sendSimpleEmail(to, subject, text);

          verify(javaMailSender, times(1)).send(any(SimpleMailMessage.class));
      }

      @Test
      @DisplayName("이메일 전송 실패 - MailException 발생 시 BusinessException 전환")
      void sendSimpleEmail_ShouldThrowBusinessExceptionOnMailFailure() {
          // given
          String to = "recipient@test.com";
          String subject = "테스트 이메일";
          String text = "테스트 내용";

          doThrow(new MailException("메일 전송 실패") {})
                  .when(javaMailSender)
                  .send(any(SimpleMailMessage.class));

          // when & then
          assertThatThrownBy(() -> emailService.sendSimpleEmail(to, subject, text))
                  .isInstanceOf(BusinessException.class)
                  .hasMessageContaining("작업 중 오류가 발생했습니다");
      }

      @Test
      @DisplayName("잘못된 이메일 주소 - BusinessException 발생")
      void sendSimpleEmail_ShouldThrowBusinessExceptionForInvalidEmail() {
          // given
          String invalidEmail = "invalid-email";
          String subject = "테스트";
          String text = "테스트";

          // when & then
          assertThatThrownBy(() -> emailService.sendSimpleEmail(invalidEmail, subject, text))
                  .isInstanceOf(BusinessException.class)
                  .hasMessageContaining("올바른 이메일 형식이 아닙니다");
      }
}
