package org.scoula.global.common.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

      private boolean success;
      private String message;
      private T data;
      private ErrorDetails error;
      private LocalDateTime timestamp;

      public static <T> ApiResponse<T> success() {
          return ApiResponse.<T>builder().success(true).timestamp(LocalDateTime.now()).build();
      }

      public static <T> ApiResponse<T> success(T data) {
          return ApiResponse.<T>builder()
                  .success(true)
                  .data(data)
                  .timestamp(LocalDateTime.now())
                  .build();
      }

      public static <T> ApiResponse<T> success(T data, String message) {
          return ApiResponse.<T>builder()
                  .success(true)
                  .data(data)
                  .message(message)
                  .timestamp(LocalDateTime.now())
                  .build();
      }

      public static <T> ApiResponse<T> error(String message) {
          return ApiResponse.<T>builder()
                  .success(false)
                  .message(message)
                  .timestamp(LocalDateTime.now())
                  .build();
      }

      public static <T> ApiResponse<T> error(String code, String message) {
          ErrorDetails errorDetails = ErrorDetails.builder().code(code).reason(message).build();
          return ApiResponse.<T>builder()
                  .success(false)
                  .message(message)
                  .error(errorDetails)
                  .timestamp(LocalDateTime.now())
                  .build();
      }

      public static <T> ApiResponse<T> error(String message, ErrorDetails error) {
          return ApiResponse.<T>builder()
                  .success(false)
                  .message(message)
                  .error(error)
                  .timestamp(LocalDateTime.now())
                  .build();
      }

      @Getter
      @Builder
      @NoArgsConstructor(access = AccessLevel.PROTECTED)
      @AllArgsConstructor(access = AccessLevel.PRIVATE)
      public static class ErrorDetails {
          private String code;
          private String field;
          private Object rejectedValue;
          private String reason;
      }
}
