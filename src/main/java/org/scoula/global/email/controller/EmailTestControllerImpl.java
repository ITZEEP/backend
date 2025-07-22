package org.scoula.global.email.controller;

import java.time.LocalDateTime;

import javax.validation.Valid;

import org.scoula.global.common.dto.ApiResponse;
import org.scoula.global.email.dto.TestEmailDto;
import org.scoula.global.email.service.EmailServiceInterface;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
@Log4j2
public class EmailTestControllerImpl implements EmailTestController {

      private final EmailServiceInterface emailService;

      /** {@inheritDoc} */
      @Override
      @PostMapping("/test")
      public ResponseEntity<ApiResponse<TestEmailDto.SendResponse>> sendTestEmail(
              @Valid @RequestBody TestEmailDto.SendRequest request) {
          try {
              emailService.sendSimpleEmail(
                      request.getTo(), "ITJib 테스트 이메일", "이것은 ITJib에서 보내는 테스트 이메일입니다.");

              TestEmailDto.SendResponse response =
                      TestEmailDto.SendResponse.builder()
                              .to(request.getTo())
                              .messageType("test")
                              .sentAt(LocalDateTime.now())
                              .build();

              return ResponseEntity.ok(ApiResponse.success(response, "테스트 이메일 전송 성공"));
          } catch (Exception e) {
              log.error("테스트 이메일 전송 실패", e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(ApiResponse.error("EMAIL_ERROR", e.getMessage()));
          }
      }

      /** {@inheritDoc} */
      @Override
      @PostMapping("/welcome")
      public ResponseEntity<ApiResponse<TestEmailDto.SendResponse>> sendWelcomeEmail(
              @Valid @RequestBody TestEmailDto.WelcomeRequest request) {
          try {
              emailService.sendWelcomeEmail(request.getTo(), request.getUsername());

              TestEmailDto.SendResponse response =
                      TestEmailDto.SendResponse.builder()
                              .to(request.getTo())
                              .name(request.getUsername())
                              .messageType("welcome")
                              .sentAt(LocalDateTime.now())
                              .build();

              return ResponseEntity.ok(ApiResponse.success(response, "환영 이메일 전송 성공"));
          } catch (Exception e) {
              log.error("환영 이메일 전송 실패", e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(ApiResponse.error("EMAIL_ERROR", e.getMessage()));
          }
      }
}
