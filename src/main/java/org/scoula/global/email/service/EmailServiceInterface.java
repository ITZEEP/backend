package org.scoula.global.email.service;

import org.scoula.global.common.exception.BusinessException;

/**
 * 이메일 전송 서비스 인터페이스
 *
 * <p>다양한 형태의 이메일 전송 기능을 정의합니다. 단순 텍스트, HTML, 첨부파일 포함, 대량 전송 등의 이메일 전송 기능을 제공합니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
public interface EmailServiceInterface {

      /**
       * 단순 텍스트 이메일을 전송합니다.
       *
       * @param to 수신자 이메일 주소
       * @param subject 이메일 제목
       * @param text 이메일 내용
       * @throws BusinessException 이메일 전송 실패 시
       */
      void sendSimpleEmail(String to, String subject, String text);

      /**
       * HTML 형식의 이메일을 전송합니다.
       *
       * @param to 수신자 이메일 주소
       * @param subject 이메일 제목
       * @param htmlContent HTML 형식의 이메일 내용
       * @throws BusinessException 이메일 전송 실패 시
       */
      void sendHtmlEmail(String to, String subject, String htmlContent);

      /**
       * 첨부파일이 포함된 이메일을 전송합니다.
       *
       * @param to 수신자 이메일 주소
       * @param subject 이메일 제목
       * @param text 이메일 내용
       * @param pathToAttachment 첨부파일 경로
       * @throws BusinessException 이메일 전송 실패 시 또는 첨부파일을 찾을 수 없을 시
       */
      void sendEmailWithAttachment(String to, String subject, String text, String pathToAttachment);

      /**
       * 여러 수신자에게 동시에 이메일을 전송합니다.
       *
       * @param recipients 수신자 이메일 주소 배열
       * @param subject 이메일 제목
       * @param text 이메일 내용
       * @throws BusinessException 이메일 전송 실패 시
       */
      void sendBulkEmail(String[] recipients, String subject, String text);

      /**
       * 신규 사용자에게 환영 이메일을 전송합니다.
       *
       * @param to 수신자 이메일 주소
       * @param username 사용자 표시 이름
       * @throws BusinessException 이메일 전송 실패 시
       */
      void sendWelcomeEmail(String to, String username);

      /**
       * 비밀번호 재설정 이메일을 전송합니다.
       *
       * @param to 수신자 이메일 주소
       * @param resetToken 비밀번호 재설정 토큰
       * @throws BusinessException 이메일 전송 실패 시
       */
      void sendPasswordResetEmail(String to, String resetToken);
}
