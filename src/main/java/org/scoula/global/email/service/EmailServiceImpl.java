package org.scoula.global.email.service;

import java.io.File;
import java.util.Arrays;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.scoula.global.common.constant.Constants;
import org.scoula.global.common.exception.BusinessException;
import org.scoula.global.common.service.AbstractExternalService;
import org.scoula.global.common.util.ValidationUtils;
import org.scoula.global.email.exception.EmailErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * 이메일 전송 서비스 구현체
 *
 * <p>다양한 형태의 이메일 전송 기능을 구현합니다. 단순 텍스트, HTML, 첨부파일 포함, 대량 전송 등의 이메일 전송 기능을 제공합니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class EmailServiceImpl extends AbstractExternalService implements EmailServiceInterface {

      private final JavaMailSender mailSender;

      @Value("${mail.from.address}")
      private String fromAddress;

      @Value("${app.frontend.url}")
      private String frontendUrl;

      private static final String DEFAULT_CHARSET = Constants.Email.DEFAULT_CHARSET;

      /** {@inheritDoc} */
      @Override
      public void sendSimpleEmail(String to, String subject, String text) {
          ValidationUtils.validateEmail(to, EmailErrorCode.INVALID_INPUT_VALUE);
          ValidationUtils.validateNotEmpty(subject, "제목", EmailErrorCode.INVALID_INPUT_VALUE);
          ValidationUtils.validateNotEmpty(text, "내용", EmailErrorCode.INVALID_INPUT_VALUE);

          executeSafely(
                  () -> {
                      SimpleMailMessage message = createSimpleMessage(to, subject, text);
                      mailSender.send(message);
                      return null;
                  },
                  "단순 이메일 전송");
      }

      /** {@inheritDoc} */
      @Override
      public void sendHtmlEmail(String to, String subject, String htmlContent) {
          validateEmailParameters(to, subject, htmlContent);

          try {
              MimeMessage message = mailSender.createMimeMessage();
              MimeMessageHelper helper = new MimeMessageHelper(message, true, DEFAULT_CHARSET);

              configureMessageHelper(helper, to, subject);
              helper.setText(htmlContent, true);

              mailSender.send(message);
              log.info("HTML 이메일 전송 성공: {}", to);
          } catch (MessagingException | MailException e) {
              log.error("HTML 이메일 전송 실패: {}", to, e);
              throw new BusinessException(EmailErrorCode.EMAIL_SEND_FAILED, "HTML 이메일 전송에 실패했습니다", e);
          }
      }

      /** {@inheritDoc} */
      @Override
      public void sendEmailWithAttachment(
              String to, String subject, String text, String pathToAttachment) {
          validateEmailParameters(to, subject, text);
          validateAttachmentPath(pathToAttachment);

          try {
              MimeMessage message = mailSender.createMimeMessage();
              MimeMessageHelper helper = new MimeMessageHelper(message, true, DEFAULT_CHARSET);

              configureMessageHelper(helper, to, subject);
              helper.setText(text);

              FileSystemResource file = new FileSystemResource(new File(pathToAttachment));
              if (!file.exists()) {
                  throw new BusinessException(
                          EmailErrorCode.FILE_NOT_FOUND, "첨부파일을 찾을 수 없습니다: " + pathToAttachment);
              }

              helper.addAttachment(file.getFilename(), file);

              mailSender.send(message);
              log.info("첨부파일 포함 이메일 전송 성공: {}", to);
          } catch (MessagingException | MailException e) {
              log.error("첨부파일 포함 이메일 전송 실패: {}", to, e);
              throw new BusinessException(
                      EmailErrorCode.EMAIL_SEND_FAILED, "첨부파일 포함 이메일 전송에 실패했습니다", e);
          }
      }

      /** {@inheritDoc} */
      @Override
      public void sendBulkEmail(String[] recipients, String subject, String text) {
          validateBulkEmailParameters(recipients, subject, text);

          try {
              SimpleMailMessage message = createSimpleMessage(null, subject, text);
              message.setTo(recipients);

              mailSender.send(message);
              log.info("대량 이메일 전송 성공: {} 명", recipients.length);
          } catch (MailException e) {
              log.error("대량 이메일 전송 실패: {} 명", recipients.length, e);
              throw new BusinessException(EmailErrorCode.EMAIL_SEND_FAILED, "대량 이메일 전송에 실패했습니다", e);
          }
      }

      /** {@inheritDoc} */
      @Override
      public void sendWelcomeEmail(String to, String username) {
          ValidationUtils.validateEmail(to, EmailErrorCode.INVALID_INPUT_VALUE);
          ValidationUtils.validateNotEmpty(username, "사용자명", EmailErrorCode.INVALID_INPUT_VALUE);

          String subject = "ITJib에 오신 것을 환영합니다!";
          String htmlContent = buildWelcomeEmailTemplate(username);

          sendHtmlEmail(to, subject, htmlContent);
      }

      /** {@inheritDoc} */
      @Override
      public void sendPasswordResetEmail(String to, String resetToken) {
          ValidationUtils.validateEmail(to, EmailErrorCode.INVALID_INPUT_VALUE);
          ValidationUtils.validateNotEmpty(resetToken, "재설정 토큰", EmailErrorCode.INVALID_INPUT_VALUE);

          String subject = "ITJib 비밀번호 재설정";
          String htmlContent = buildPasswordResetEmailTemplate(resetToken);

          sendHtmlEmail(to, subject, htmlContent);
      }

      // 개인 헬퍼 메서드

      private SimpleMailMessage createSimpleMessage(String to, String subject, String text) {
          SimpleMailMessage message = new SimpleMailMessage();
          if (StringUtils.hasText(to)) {
              message.setTo(to);
          }
          message.setSubject(subject);
          message.setText(text);
          message.setFrom(fromAddress);
          return message;
      }

      private void configureMessageHelper(MimeMessageHelper helper, String to, String subject)
              throws MessagingException {
          helper.setTo(to);
          helper.setSubject(subject);
          helper.setFrom(fromAddress);
      }

      private String buildWelcomeEmailTemplate(String username) {
          return String.format(
                  "<html>"
                          + "<body>"
                          + "<h2>안녕하세요, %s님!</h2>"
                          + "<p>ITJib 회원이 되신 것을 진심으로 환영합니다.</p>"
                          + "<p>저희 서비스를 이용해 주셔서 감사합니다.</p>"
                          + "<br>"
                          + "<p>문의사항이 있으시면 언제든지 연락주세요.</p>"
                          + "<p>감사합니다.</p>"
                          + "</body>"
                          + "</html>",
                  username);
      }

      private String buildPasswordResetEmailTemplate(String resetToken) {
          String resetUrl = frontendUrl + "/reset-password?token=" + resetToken;
          return String.format(
                  "<html>"
                          + "<body>"
                          + "<h2>비밀번호 재설정</h2>"
                          + "<p>비밀번호를 재설정하려면 아래 링크를 클릭하세요:</p>"
                          + "<p><a href='%s'>비밀번호 재설정</a></p>"
                          + "<p>이 링크는 24시간 후에 만료됩니다.</p>"
                          + "<br>"
                          + "<p>본인이 요청하지 않으셨다면 이 메일을 무시하세요.</p>"
                          + "</body>"
                          + "</html>",
                  resetUrl);
      }

      // 검증 메서드

      private void validateEmailParameters(String to, String subject, String content) {
          ValidationUtils.validateEmail(to, EmailErrorCode.INVALID_INPUT_VALUE);
          ValidationUtils.validateNotEmpty(subject, "제목", EmailErrorCode.INVALID_INPUT_VALUE);
          ValidationUtils.validateNotEmpty(content, "내용", EmailErrorCode.INVALID_INPUT_VALUE);
      }

      private void validateBulkEmailParameters(String[] recipients, String subject, String content) {
          if (recipients == null || recipients.length == 0) {
              throw new BusinessException(EmailErrorCode.INVALID_INPUT_VALUE, "수신자가 비어있을 수 없습니다");
          }

          Arrays.stream(recipients)
                  .forEach(
                          email ->
                                  ValidationUtils.validateEmail(
                                          email, EmailErrorCode.INVALID_INPUT_VALUE));
          ValidationUtils.validateNotEmpty(subject, "제목", EmailErrorCode.INVALID_INPUT_VALUE);
          ValidationUtils.validateNotEmpty(content, "내용", EmailErrorCode.INVALID_INPUT_VALUE);
      }

      private void validateAttachmentPath(String path) {
          ValidationUtils.validateNotEmpty(path, "첨부파일 경로", EmailErrorCode.INVALID_INPUT_VALUE);
      }
}
