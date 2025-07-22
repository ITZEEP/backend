package org.scoula.global.email.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scoula.global.common.dto.ApiResponse;
import org.scoula.global.common.exception.BusinessException;
import org.scoula.global.common.exception.CommonErrorCode;
import org.scoula.global.email.dto.TestEmailDto;
import org.scoula.global.email.service.EmailServiceInterface;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
@DisplayName("이메일 테스트 컨트롤러 구현체 단위 테스트")
class EmailTestControllerImplTest {

      @Mock private EmailServiceInterface emailService;

      @InjectMocks private EmailTestControllerImpl emailTestController;

      @Test
      @DisplayName("테스트 이메일 전송 - 성공")
      void sendTestEmail_ShouldReturnSuccessResponse() {
          // given
          TestEmailDto.SendRequest request =
                  TestEmailDto.SendRequest.builder().to("test@example.com").build();

          doNothing()
                  .when(emailService)
                  .sendSimpleEmail(
                          eq("test@example.com"),
                          eq("ITJib 테스트 이메일"),
                          eq("이것은 ITJib에서 보내는 테스트 이메일입니다."));

          // when
          ResponseEntity<ApiResponse<TestEmailDto.SendResponse>> response =
                  emailTestController.sendTestEmail(request);

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody().isSuccess()).isTrue();

          TestEmailDto.SendResponse data = response.getBody().getData();
          assertThat(data.getTo()).isEqualTo("test@example.com");
          assertThat(data.getMessageType()).isEqualTo("test");
          assertThat(data.getSentAt()).isNotNull();

          verify(emailService)
                  .sendSimpleEmail(
                          "test@example.com", "ITJib 테스트 이메일", "이것은 ITJib에서 보내는 테스트 이메일입니다.");
      }

      @Test
      @DisplayName("테스트 이메일 전송 - 실패")
      void sendTestEmail_ShouldReturnErrorResponse_WhenEmailServiceFails() {
          // given
          TestEmailDto.SendRequest request =
                  TestEmailDto.SendRequest.builder().to("invalid@email").build();

          doThrow(new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, "이메일 전송 실패"))
                  .when(emailService)
                  .sendSimpleEmail(anyString(), anyString(), anyString());

          // when
          ResponseEntity<ApiResponse<TestEmailDto.SendResponse>> response =
                  emailTestController.sendTestEmail(request);

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody().isSuccess()).isFalse();
          assertThat(response.getBody().getMessage()).contains("이메일 전송 실패");
      }

      @Test
      @DisplayName("환영 이메일 전송 - 성공")
      void sendWelcomeEmail_ShouldReturnSuccessResponse() {
          // given
          TestEmailDto.WelcomeRequest request =
                  TestEmailDto.WelcomeRequest.builder()
                          .to("newuser@example.com")
                          .username("신규사용자")
                          .build();

          doNothing().when(emailService).sendWelcomeEmail("newuser@example.com", "신규사용자");

          // when
          ResponseEntity<ApiResponse<TestEmailDto.SendResponse>> response =
                  emailTestController.sendWelcomeEmail(request);

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody().isSuccess()).isTrue();

          TestEmailDto.SendResponse data = response.getBody().getData();
          assertThat(data.getTo()).isEqualTo("newuser@example.com");
          assertThat(data.getName()).isEqualTo("신규사용자");
          assertThat(data.getMessageType()).isEqualTo("welcome");
          assertThat(data.getSentAt()).isNotNull();

          verify(emailService).sendWelcomeEmail("newuser@example.com", "신규사용자");
      }

      @Test
      @DisplayName("환영 이메일 전송 - 실패")
      void sendWelcomeEmail_ShouldReturnErrorResponse_WhenEmailServiceFails() {
          // given
          TestEmailDto.WelcomeRequest request =
                  TestEmailDto.WelcomeRequest.builder()
                          .to("invalid@email")
                          .username("테스트사용자")
                          .build();

          doThrow(new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, "환영 이메일 전송 실패"))
                  .when(emailService)
                  .sendWelcomeEmail(anyString(), anyString());

          // when
          ResponseEntity<ApiResponse<TestEmailDto.SendResponse>> response =
                  emailTestController.sendWelcomeEmail(request);

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody().isSuccess()).isFalse();
          assertThat(response.getBody().getMessage()).contains("환영 이메일 전송 실패");
      }

      @Test
      @DisplayName("환영 이메일 전송 - 빈 사용자명")
      void sendWelcomeEmail_ShouldHandleEmptyUsername() {
          // given
          TestEmailDto.WelcomeRequest request =
                  TestEmailDto.WelcomeRequest.builder().to("user@example.com").username("").build();

          doNothing().when(emailService).sendWelcomeEmail("user@example.com", "");

          // when
          ResponseEntity<ApiResponse<TestEmailDto.SendResponse>> response =
                  emailTestController.sendWelcomeEmail(request);

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
          TestEmailDto.SendResponse data = response.getBody().getData();
          assertThat(data.getName()).isEqualTo("");
      }
}
