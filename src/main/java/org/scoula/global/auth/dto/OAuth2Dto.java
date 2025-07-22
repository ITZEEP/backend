package org.scoula.global.auth.dto;

import java.util.List;
import java.util.Map;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class OAuth2Dto {

      @Getter
      @Builder
      @NoArgsConstructor(access = AccessLevel.PROTECTED)
      @AllArgsConstructor(access = AccessLevel.PRIVATE)
      @ApiModel(description = "카카오 로그인 URL 응답")
      public static class LoginUrlResponse {
          @ApiModelProperty(
                  value = "카카오 로그인 URL",
                  example = "http://localhost:8080/oauth2/authorization/kakao")
          private String loginUrl;

          @ApiModelProperty(value = "HTTP 메서드", example = "GET")
          private String method;

          @ApiModelProperty(value = "설명", example = "브라우저에서 해당 URL로 접속하면 카카오 로그인 페이지로 리다이렉트됩니다.")
          private String description;
      }

      @Getter
      @Builder
      @NoArgsConstructor(access = AccessLevel.PROTECTED)
      @AllArgsConstructor(access = AccessLevel.PRIVATE)
      @ApiModel(description = "사용자 정보 응답")
      public static class UserInfoResponse {
          @ApiModelProperty(value = "인증 제공자", example = "kakao")
          private String provider;

          @ApiModelProperty(value = "사용자 ID", example = "123456789")
          private String id;

          @ApiModelProperty(value = "사용자명", example = "홍길동")
          private String username;

          @ApiModelProperty(value = "이메일", example = "user@example.com")
          private String email;

          @ApiModelProperty(value = "닉네임", example = "길동이")
          private String nickname;

          @ApiModelProperty(value = "프로필 이미지 URL")
          private String profileImage;

          @ApiModelProperty(value = "권한 목록")
          private List<String> authorities;

          @ApiModelProperty(value = "OAuth2 원본 속성 (kakao 사용자만)")
          private Map<String, Object> attributes;
      }

      @Getter
      @Builder
      @NoArgsConstructor(access = AccessLevel.PROTECTED)
      @AllArgsConstructor(access = AccessLevel.PRIVATE)
      @ApiModel(description = "인증 상태 응답")
      public static class AuthStatusResponse {
          @ApiModelProperty(value = "인증 여부", example = "true")
          private boolean authenticated;

          @ApiModelProperty(value = "사용자명/이름", example = "홍길동")
          private String name;

          @ApiModelProperty(value = "인증 타입", example = "oauth2", allowableValues = "oauth2,jwt")
          private String type;

          @ApiModelProperty(value = "OAuth2 제공자", example = "kakao")
          private String provider;

          @ApiModelProperty(value = "권한 목록")
          private List<String> authorities;
      }
}
