package org.scoula.global.auth.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class KakaoUserInfo {
      private Map<String, Object> attributes;

      public String getId() {
          return String.valueOf(attributes.get("id"));
      }

      public String getEmail() {
          Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
          if (kakaoAccount == null) {
              return null;
          }
          return (String) kakaoAccount.get("email");
      }

      public String getNickname() {
          Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
          if (kakaoAccount == null) {
              return null;
          }

          Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
          if (profile == null) {
              return null;
          }

          return (String) profile.get("nickname");
      }

      public String getProfileImageUrl() {
          Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
          if (kakaoAccount == null) {
              return null;
          }

          Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
          if (profile == null) {
              return null;
          }

          return (String) profile.get("profile_image_url");
      }

      public String getThumbnailImageUrl() {
          Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
          if (kakaoAccount == null) {
              return null;
          }

          Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
          if (profile == null) {
              return null;
          }

          return (String) profile.get("thumbnail_image_url");
      }

      public String getGender() {
          Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
          if (kakaoAccount == null) {
              return null;
          }
          return (String) kakaoAccount.get("gender");
      }

      public String getAgeRange() {
          Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
          if (kakaoAccount == null) {
              return null;
          }
          return (String) kakaoAccount.get("age_range");
      }
}
