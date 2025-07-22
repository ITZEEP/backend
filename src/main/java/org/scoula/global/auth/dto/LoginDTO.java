package org.scoula.global.auth.dto;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginDTO {
      private String username;
      private String password;

      public static LoginDTO of(HttpServletRequest request) {
          ObjectMapper objectMapper = new ObjectMapper();
          try {
              return objectMapper.readValue(request.getInputStream(), LoginDTO.class);
          } catch (IOException e) {
              throw new RuntimeException("Failed to parse login request", e);
          }
      }
}
