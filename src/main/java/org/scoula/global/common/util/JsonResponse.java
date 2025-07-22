package org.scoula.global.common.util;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonResponse {

      private static final ObjectMapper objectMapper = new ObjectMapper();

      public static void sendError(HttpServletResponse response, HttpStatus status, String message)
              throws IOException {
          response.setStatus(status.value());
          response.setContentType(MediaType.APPLICATION_JSON_VALUE);
          response.setCharacterEncoding("UTF-8");

          ErrorResponse errorResponse = new ErrorResponse(status.value(), message);
          String json = objectMapper.writeValueAsString(errorResponse);
          response.getWriter().write(json);
      }

      public static void sendSuccess(HttpServletResponse response, Object data) throws IOException {
          response.setStatus(HttpStatus.OK.value());
          response.setContentType(MediaType.APPLICATION_JSON_VALUE);
          response.setCharacterEncoding("UTF-8");

          String json = objectMapper.writeValueAsString(data);
          response.getWriter().write(json);
      }

      private static class ErrorResponse {
          private int status;
          private String message;

          public ErrorResponse(int status, String message) {
              this.status = status;
              this.message = message;
          }

          public int getStatus() {
              return status;
          }

          public String getMessage() {
              return message;
          }
      }
}
