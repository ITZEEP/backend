package org.scoula.global.email.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class TestEmailDto {

      @Getter
      @Builder
      @NoArgsConstructor(access = AccessLevel.PROTECTED)
      @AllArgsConstructor(access = AccessLevel.PRIVATE)
      @ApiModel(description = "직접 이메일 전송 요청")
      public static class SendRequest {
          @NotBlank(message = "이메일 주소는 필수입니다")
          @Email(message = "올바른 이메일 형식이 아닙니다")
          @ApiModelProperty(value = "수신자 이메일", example = "test@example.com")
          private String to;
      }

      @Getter
      @Builder
      @NoArgsConstructor(access = AccessLevel.PROTECTED)
      @AllArgsConstructor(access = AccessLevel.PRIVATE)
      @ApiModel(description = "환영 이메일 전송 요청")
      public static class WelcomeRequest {
          @NotBlank(message = "이메일 주소는 필수입니다")
          @Email(message = "올바른 이메일 형식이 아닙니다")
          @ApiModelProperty(value = "수신자 이메일", example = "test@example.com")
          private String to;

          @NotBlank(message = "사용자명은 필수입니다")
          @ApiModelProperty(value = "사용자명", example = "홍길동")
          private String username;
      }

      @Getter
      @Builder
      @NoArgsConstructor(access = AccessLevel.PROTECTED)
      @AllArgsConstructor(access = AccessLevel.PRIVATE)
      @ApiModel(description = "이메일 전송 결과")
      public static class SendResponse {
          @ApiModelProperty(value = "수신자 이메일", example = "test@example.com")
          private String to;

          @ApiModelProperty(value = "수신자 이름", example = "홍길동")
          private String name;

          @ApiModelProperty(value = "메시지 타입", example = "test")
          private String messageType;

          @ApiModelProperty(value = "전송 시각", example = "2024-01-15T10:30:00")
          private java.time.LocalDateTime sentAt;
      }
}
